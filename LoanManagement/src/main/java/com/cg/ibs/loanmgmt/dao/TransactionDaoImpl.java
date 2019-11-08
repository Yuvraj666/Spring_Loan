package com.cg.ibs.loanmgmt.dao;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import com.cg.ibs.loanmgmt.bean.LoanMaster;
import com.cg.ibs.loanmgmt.bean.TransactionBean;
import com.cg.ibs.loanmgmt.bean.TransactionMode;
import com.cg.ibs.loanmgmt.bean.TransactionType;
import com.cg.ibs.loanmgmt.util.JpaUtil;

public class TransactionDaoImpl implements TransactionDao {
	private EntityManager entityManager;

	public TransactionDaoImpl() {
		entityManager = JpaUtil.getEntityManger();
	}

	@Override
	public TransactionBean createTransaction(LoanMaster loanMaster,TransactionBean transaction) {
		transaction.setAccountNumber(loanMaster.getLoanAccountNumber());
		transaction.setTransactionAmount(loanMaster.getEmiAmount());
		transaction.setTransactionDate(LocalDateTime.now());
		transaction.setTransactionDescription("Emi Payment: "+ loanMaster.getLoanAccountNumber() +" Emi Number: "+loanMaster.getNumOfEmisPaid());
		transaction.setTransactionMode(TransactionMode.ONLINE);
		transaction.setTransactionType(TransactionType.DEBIT);
		entityManager.persist(transaction);
		return transaction;
	}

}
