package com.example.demo.models;

import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.springframework.stereotype.Repository;

import com.example.demo.bean.Account;


@Repository
@Transactional
public class AccountDao {
	/** * Save the user in the database. */
	public void create(Account user) {
		entityManager.persist(user);
		return;
	}

	/** * Delete the user from the database. */
	public void delete(Account user) {
		if (entityManager.contains(user))
			entityManager.remove(user);
		else
			entityManager.remove(entityManager.merge(user));
		return;
	}

	/** * Return all the users stored in the database. */
	@SuppressWarnings("unchecked")
	public List getAll() {
		return entityManager.createQuery("from Employee").getResultList();
	}

	/** * Return the user having the passed email. */
	public Account getByEmail(String email) {
		return (Account) entityManager.createQuery("from Employee where email = :email").setParameter("email", email)
				.getSingleResult();
	}

	/** * Return the user having the passed id. */
	public Account getById(long id) {
		return entityManager.find(Account.class, id);
	}

	/** * Update the passed user in the database. */
	public void update(Account user) {
		entityManager.merge(user);
		return;
	}

	// Private fields
	// An EntityManager will be automatically injected from
	// entityManagerFactory
	// setup on DatabaseConfig class. @PersistenceContext
	private EntityManager entityManager;

}
