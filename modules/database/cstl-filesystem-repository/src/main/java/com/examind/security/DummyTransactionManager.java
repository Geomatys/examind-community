/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.examind.security;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 *
 * @author guilhem
 */
public class DummyTransactionManager implements PlatformTransactionManager {

    @Override
    public TransactionStatus getTransaction(TransactionDefinition td) throws TransactionException {
        return null;
    }

    @Override
    public void commit(TransactionStatus ts) throws TransactionException {
        //do nothing
    }

    @Override
    public void rollback(TransactionStatus ts) throws TransactionException {
        //do nothing
    }

}
