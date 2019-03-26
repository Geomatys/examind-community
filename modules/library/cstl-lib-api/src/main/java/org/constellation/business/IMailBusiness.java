/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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

package org.constellation.business;

import java.nio.file.Path;
import java.util.List;
import org.constellation.exception.ConstellationException;

/**
 * Created with IntelliJ IDEA.
 * User: laurent
 * Date: 29/04/15
 * Time: 15:59
 * Geomatys
 */
public interface IMailBusiness {

    /**
     * Sends an email with the specified subject and HTML message to the specified
     * recipients.
     *
     * @param subject    the subject
     * @param htmlMsg    the html message
     * @param recipients the recipients addresses
     * @throws ConstellationException if an error occurred while creating or sending the email
     */
    void send(String subject, String htmlMsg, List<String> recipients) throws ConstellationException;

    void send(String subject, String htmlMsg, List<String> recipients, Path attachment) throws ConstellationException;
}
