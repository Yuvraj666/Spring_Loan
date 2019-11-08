package com.cg.ibs.loanmgmt.dao;

import com.cg.ibs.loanmgmt.bean.LoanMaster;
import com.cg.ibs.loanmgmt.bean.TransactionBean;

public interface TransactionDao {
TransactionBean createTransaction(LoanMaster loanMaster, TransactionBean transaction);
}
