/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.examind.wps;

import com.examind.wps.api.UnknowJobException;
import com.examind.wps.api.UnknowQuotationException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.geotoolkit.wps.xml.v200.Bill;
import org.geotoolkit.wps.xml.v200.Quotation;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class QuotationInfo {

    private final long expiration;
    private final Timer timer = new Timer(true);

    private final Map<String, Quotation> quotationMap = new HashMap<>();
    private final Map<String, Quotation> historyQuotationMap = new HashMap<>(); // contains expired quotation used fo billing
    private final Map<String, List<String>> processQuotationMap = new HashMap<>();
    private final Map<String, Bill> billMap = new HashMap<>();
    private final Map<String, Bill> jobBillMap = new HashMap<>();
    private final Set<String> expiredQuotation = new HashSet<>();

    private static final Logger LOGGER = Logging.getLogger("com.examind.wps");

    public QuotationInfo() {
        final long defaultExpire = 1000*60*60*24*7;  // une semaine
        expiration = Application.getLongProperty(AppProperty.EXA_QUOTATION_EXPIRE, defaultExpire);
    }

    public Quotation getQuotation(String quoteId) throws UnknowQuotationException {
        if (expiredQuotation.contains(quoteId)) {
            throw new UnknowQuotationException("The quotation " + quoteId + " has expired.");
        }
        if (!quotationMap.containsKey(quoteId)) {
            throw new UnknowQuotationException("There is no quotation registrered in the service with the id:" + quoteId);
        }
        return quotationMap.get(quoteId);
    }

    public List<String> getQuotations(String processId) {
        if (!processQuotationMap.containsKey(processId)) {
            return new ArrayList<>();
        }
        return processQuotationMap.get(processId);
    }

    public List<String> getAllQuotationIds() {
        List<String> results = new ArrayList<>();
        for ( List<String> quoteIds : processQuotationMap.values()) {
            results.addAll(quoteIds);
        }
        return results;
    }

    public List<String> getAllBillIds() {
        return new ArrayList<>(billMap.keySet());
    }

    public Bill getBill(String billId) throws UnknowQuotationException {
        if (!billMap.containsKey(billId)) {
            throw new UnknowQuotationException("There is no bill registrered in the service with the id:" + billId);
        }
        return billMap.get(billId);
    }

    public Bill getBillForJob(String jobId) throws UnknowJobException {
        if (!jobBillMap.containsKey(jobId)) {
            throw new UnknowJobException("There is no job with a biull registrered in the service with the id:" + jobId);
        }
        return jobBillMap.get(jobId);
    }

    public void addQuotation(Quotation quote) {
        quote.setCreated(new Date());
        Date expireDate = new Date(System.currentTimeMillis() + expiration);
        quote.setExpire(expireDate);
        QuotationExpirationTask expireTask = new QuotationExpirationTask(quote);
        timer.schedule(expireTask, expireDate);

        quotationMap.put(quote.getId(), quote);
        historyQuotationMap.put(quote.getId(), quote);
        String processId = quote.getProcessId();
        if (processQuotationMap.containsKey(processId)) {
            processQuotationMap.get(processId).add(quote.getId());
        } else {
            List<String> ids = new ArrayList<>();
            ids.add(quote.getId());
            processQuotationMap.put(processId, ids);
        }
    }

    public void expireQuotation(Quotation quote) {
        expiredQuotation.add(quote.getId());
        quotationMap.remove(quote.getId());
        String processId = quote.getProcessId();
        if (processQuotationMap.containsKey(processId)) {
            processQuotationMap.get(processId).remove(quote.getId());
        }
    }

    public String addBill(String quoteId, String jobId) {
       if (historyQuotationMap.containsKey(quoteId)) {
           Quotation quote = historyQuotationMap.get(quoteId);
           String billId = UUID.randomUUID().toString();
           Bill bill = new Bill(billId, quote);
           bill.setCreated(new Date());
           billMap.put(billId, bill);
           jobBillMap.put(jobId, bill);
       }
       return null;
    }

    public class QuotationExpirationTask extends TimerTask {

        private final Quotation quote;

        public QuotationExpirationTask(Quotation quote) {
            this.quote = quote;
        }

        @Override
        public void run() {
            LOGGER.log(Level.INFO, "{0} quotation has expired.", quote.getId());
            expireQuotation(quote);
        }
    }
}
