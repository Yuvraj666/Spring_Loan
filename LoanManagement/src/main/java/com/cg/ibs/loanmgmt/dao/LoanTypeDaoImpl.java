package com.cg.ibs.loanmgmt.dao;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import com.cg.ibs.loanmgmt.bean.LoanTypeBean;
import com.cg.ibs.loanmgmt.util.JpaUtil;

public class LoanTypeDaoImpl implements LoanTypeDao{
	private static Logger LOGGER = Logger.getLogger(LoanTypeDaoImpl.class);
	private LoanTypeBean loanTypeBean;
	private EntityManager entityManager;
	
	public LoanTypeDaoImpl() {
		entityManager = JpaUtil.getEntityManger();
	}
	public LoanTypeBean getLoanTypeByTypeID(Integer typeId) {
		return entityManager.find(LoanTypeBean.class, typeId);
	}

	
}