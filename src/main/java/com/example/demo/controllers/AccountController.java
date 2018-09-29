package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.bean.Account;
import com.example.demo.models.AccountDao;

@Controller
public class AccountController {

	/**
	 * Create a new user with an auto-generated id and email and name as passed
	 * values.
	 */
	@RequestMapping(value = "/create")
	@ResponseBody
	public String create(String email, String name) {
		try {
			System.out.println("2018年9月28日下午4:52:34->" + email + name);
			Account user = new Account(email, name);
			user.setId(1L);
			userDao.create(user);
		} catch (Exception ex) {
			ex.printStackTrace();
			return "Error creating the user: " + ex.toString();
		}
		return "User succesfully created!";
	}

	/**
	 * Delete the user with the passed id.
	 */
	@RequestMapping(value = "/delete")
	@ResponseBody
	public String delete(long id) {
		try {
			Account user = new Account(id);
			userDao.delete(user);
		} catch (Exception ex) {
			return "Error deleting the user: " + ex.toString();
		}
		return "User succesfully deleted!";
	}

	/**
	 * Retrieve the id for the user with the passed email address.
	 */
	@RequestMapping(value = "/get-by-email")
	@ResponseBody
	public String getByEmail(String email) {
		String userId;
		try {
			Account user = userDao.getByEmail(email);
			userId = String.valueOf(user.getId());
		} catch (Exception ex) {
			return "User not found: " + ex.toString();
		}
		return "The user id is: " + userId;
	}

	/**
	 * Update the email and the name for the user indentified by the passed id.
	 */
	@RequestMapping(value = "/update")
	@ResponseBody
	public String updateName(long id, String email, String name) {
		try {
			Account user = userDao.getById(id);
			user.setEmail(email);
			user.setName(name);
			userDao.update(user);
		} catch (Exception ex) {
			return "Error updating the user: " + ex.toString();
		}
		return "User succesfully updated!";
	}

	// Private fields

	// Wire the UserDao used inside this controller.
	@Autowired
	private AccountDao userDao;

}
