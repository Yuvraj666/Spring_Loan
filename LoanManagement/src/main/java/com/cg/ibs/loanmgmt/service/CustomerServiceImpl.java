package com.cg.ibs.loanmgmt.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.cg.ibs.loanmgmt.IBSexception.IBSException;
import com.cg.ibs.loanmgmt.bean.CustomerBean;
import com.cg.ibs.loanmgmt.bean.LoanMaster;
import com.cg.ibs.loanmgmt.bean.LoanStatus;
import com.cg.ibs.loanmgmt.bean.LoanTypeBean;
import com.cg.ibs.loanmgmt.bean.TransactionBean;
import com.cg.ibs.loanmgmt.dao.CustomerDao;
import com.cg.ibs.loanmgmt.dao.CustomerDaoImpl;
import com.cg.ibs.loanmgmt.dao.LoanMasterDao;
import com.cg.ibs.loanmgmt.dao.LoanMasterDaoImpl;
import com.cg.ibs.loanmgmt.dao.LoanTypeDao;
import com.cg.ibs.loanmgmt.dao.LoanTypeDaoImpl;
import com.cg.ibs.loanmgmt.dao.TransactionDao;
import com.cg.ibs.loanmgmt.dao.TransactionDaoImpl;
import com.cg.ibs.loanmgmt.util.JpaUtil;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

public class CustomerServiceImpl implements CustomerService {
	private static Logger LOGGER = Logger.getLogger(CustomerServiceImpl.class);
	LoanTypeDao loanTypeDao = new LoanTypeDaoImpl();
	CustomerDao customerDao = new CustomerDaoImpl();
	LoanMasterDao loanMasterDao = new LoanMasterDaoImpl();
	LoanMaster loanMaster = new LoanMaster();

	@Override
	public LoanTypeBean getLoanTypeByTypeId(Integer typeId) {
		LOGGER.info("Fetching loan type details");
		return loanTypeDao.getLoanTypeByTypeID(typeId);
	}

	@Override
	public boolean verifyLoanAmount(BigDecimal loanAmount, BigDecimal maximumLimit, BigDecimal minimumLimit) {
		LOGGER.info("Verifying loan amount");
		boolean verify = false;
		if (loanAmount.compareTo(maximumLimit) < 0 && loanAmount.compareTo(minimumLimit) > 0) {
			verify = true;
		}
		return verify;
	}

	@Override
	public boolean verifyLoanTenure(int tenure) {
		LOGGER.info("Verifying loan tenure");
		boolean verify = false;
		if (tenure > 0 && (tenure % 6 == 0)) {
			verify = true;
		}
		return verify;
	}

	@Override
	public LoanMaster calculateEmi(LoanMaster loanMaster) {
		LOGGER.info("Calculating EMI amount");
		Float rate = loanTypeDao.getLoanTypeByTypeID(loanMaster.getTypeId()).getInterestRate() / (12 * 100);
		Double onePlusRPoweredN = Math.pow((rate + 1), loanMaster.getLoanTenure());
		BigDecimal principal = loanMaster.getLoanAmount();
		BigDecimal emi = principal.multiply(BigDecimal.valueOf((rate * onePlusRPoweredN) / (onePlusRPoweredN - 1)));
		loanMaster.setEmiAmount(emi.setScale(2, BigDecimal.ROUND_UP));
		return loanMaster;
	}

	@Override
	public boolean verifyCustomerLogin(String userId, String password) {
		LOGGER.info("Verifying customer login details");
		boolean login = false;
		CustomerBean customer = customerDao.getCustomerByUserId(userId);
		if (null != customer && password.equals(customer.getPassword())) {
			login = true;
		}
		return login;
	}

	@Override
	public CustomerBean getCustomer(String userId) {
		LOGGER.info("Fetching customer details");
		return customerDao.getCustomerByUserId(userId);
	}

	@Override
	public LoanMaster applyLoan(CustomerBean customer, LoanMaster loanMaster, String path) throws IOException {
		LOGGER.info("Loan is being applied by the customer");
		EntityTransaction transaction = JpaUtil.getTransaction();
		loanMaster.setUci(customer.getUci());
		loanMaster.setAppliedDate(LocalDate.now());
		loanMaster.setBalance(loanMaster.getLoanAmount());
		loanMaster.setStatus(LoanStatus.PENDING);
		loanMaster.setDocument(uploadDocument(path));
		transaction.begin();
		LoanMaster appliedLoan = loanMasterDao.applyLoan(loanMaster);
		transaction.commit();
		return appliedLoan;
	}

	private static byte[] uploadDocument(String path) throws IOException {
		LOGGER.info("Document is being uploaded for the applied loan");
		byte[] content = null;
		FileInputStream inStream = new FileInputStream(path);
		content = new byte[(int) inStream.available()];
		inStream.read(content);
		inStream.close();

		return content;
	}

	public List<LoanMaster> getLoanListByUci(CustomerBean customer) {
		LOGGER.info("Loan details are being fetched using uci");
		List<LoanMaster> loanList = new ArrayList<>();
		loanList = loanMasterDao.getLoanListByUci(customer);
//		for (LoanMaster loanMaster : loanList) {
//			if (loanMaster.getStatus().equals(LoanStatus.PENDING)) {
//				loanMaster.setLoanAccountNumber(BigInteger.valueOf(""));
//				loanMaster.setNumOfEmisPaid(Integer.parseInt("0"));
//			}
//		}
		return loanList;
	}

	public LoanTypeBean getLoanTypeByTypeID(Integer typeId) {
		LOGGER.info("Loan type details are being fetched using type ID");
		return loanTypeDao.getLoanTypeByTypeID(typeId);
	}

	public BigDecimal calculateBalance(LoanMaster loanMaster) {
		LOGGER.info("Balance is being calculated");
		BigDecimal initialBalance = loanMaster.getBalance();
		Float rate = getLoanTypeByTypeID(loanMaster.getTypeId()).getInterestRate();
		BigDecimal paidInterest = (initialBalance
				.multiply(BigDecimal.valueOf((Math.pow(1 + rate / 100.0, 1.0 / 12.0)))))
						.subtract(loanMaster.getBalance());
		BigDecimal paidPrincipal = loanMaster.getEmiAmount().subtract(paidInterest);
		BigDecimal newBalance = loanMaster.getBalance().subtract(paidPrincipal);
		return newBalance;
	}

	public LoanMaster updateEMI(LoanMaster loanMaster) {
		LOGGER.info("Loan details are being updated after paying EMI");
		EntityTransaction transaction = JpaUtil.getTransaction();
		loanMaster.setBalance(calculateBalance(loanMaster).setScale(2, RoundingMode.HALF_UP));

		transaction.begin();
		LoanMaster loanMasterTemp = loanMasterDao.updateEmiDao(loanMaster);
		transaction.commit();
		if (closeLoan(loanMaster)) {
			loanMaster.setStatus(LoanStatus.CLOSED);
		}
		if (loanMaster.getNumOfEmisPaid() == loanMaster.getTotalNumOfEmis() - 1) {
			loanMaster.setBalance(loanMaster.getEmiAmount());
		}
		return loanMasterTemp;
	}

	private boolean closeLoan(LoanMaster loanMaster) {
		LOGGER.info("Loan status is being updated to closed");
		boolean close = false;
		if (loanMaster.getNumOfEmisPaid() == loanMaster.getTotalNumOfEmis()) {
			close = true;
		}
		return close;
	}

	public LoanMaster updateBalance(LoanMaster loanMaster) {
		LOGGER.info("Balance is being updated");
		EntityTransaction transaction = JpaUtil.getTransaction();
		BigDecimal balance = calculateBalance(loanMaster);
		transaction.begin();
		LoanMaster loanMasterTemp = loanMasterDao.updateBalanceDao(loanMaster, balance);
		transaction.commit();
		return loanMasterTemp;
	}

	@Override
	public TransactionBean createTransaction(LoanMaster loanMaster) {
		LOGGER.info("Transaction has been created.");
		TransactionDao transactionDao = new TransactionDaoImpl();
		TransactionBean transaction = new TransactionBean();
		EntityTransaction txn = JpaUtil.getTransaction();
		txn.begin();
		transaction = transactionDao.createTransaction(loanMaster, transaction);
		txn.commit();
		return transaction;
	}

	public boolean verifyLoanExist(BigInteger loanAccNumber) {
		LOGGER.info("Verifying if the loan exist.");
		boolean check = false;
		loanMaster = loanMasterDao.getLoanByLoanNumber(loanAccNumber);
		if (null != loanMaster) {
			check = true;
		}
		return check;
	}

	public boolean verifyPreClosureApplicable(BigInteger loanAccNum) {
		LOGGER.info("Verifying the preClosure applicability.");
		boolean check = false;
		loanMaster = loanMasterDao.getLoanByLoanNumber(loanAccNum);
		if (loanMaster.getNumOfEmisPaid() >= 0.25 * loanMaster.getTotalNumOfEmis()) {
			check = true;
		}
		return check;

	}

	public void updatePreClosure(LoanMaster loanMaster) {
		LOGGER.info("Updating the preClosure amount.");
		EntityTransaction transaction = JpaUtil.getTransaction();
		transaction.begin();
		loanMasterDao.updatePreClosureDao(loanMaster);
		transaction.commit();
	}

	public LoanMaster getLoanDetails(BigInteger loanAccNumber) {
		LOGGER.info("Loan Details are being fetched using loan number");
		return loanMasterDao.getLoanByLoanNumber(loanAccNumber);
	}

	public boolean receiptGenerator(LoanMaster loanMaster, TransactionBean transaction)
			throws DocumentException, FileNotFoundException {
		LOGGER.info("Receipt is being generated");
		boolean check = false;
		Document document = new Document();
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(
				"Receipt_" + loanMaster.getLoanAccountNumber() + "_" + loanMaster.getNumOfEmisPaid() + "  _Emi.pdf"));
		document.open();

		document.add(new Paragraph("                        **********Acknowledgment Receipt**********"));
		document.add(new Paragraph("      "));
		document.add(new Paragraph("      "));
		document.add(new Paragraph("Transaction ID:      " + transaction.getTransactionId()));
		document.add(new Paragraph("Transaction Date:      " + transaction.getTransactionDate()));
		document.add(new Paragraph("Transaction Amount:      " + transaction.getTransactionAmount()));
		document.add(new Paragraph("Transaction Description:      " + transaction.getTransactionDescription()));
		document.add(new Paragraph("Transaction Mode:      " + transaction.getTransactionMode()));
		document.add(new Paragraph("Transaction Type:      " + transaction.getTransactionType()));
		document.close();
		writer.close();
		check = true;
		return check;
	}

	@Override
	public List<LoanMaster> getApprovedLoanListByUci(CustomerBean customer) {
		return loanMasterDao.getApprovedLoanListByUci(customer);
	}
}
