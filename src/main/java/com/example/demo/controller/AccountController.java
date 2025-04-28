package com.example.demo.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.example.demo.bean.Account;
import com.example.demo.models.AccountDao;

@Api("Account相关的操作接口")
@RestController("account")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final AccountDao userDao;

    public AccountController(AccountDao userDao) {
        this.userDao = userDao;
    }

    @ApiOperation(value = "根据id查询学生信息", notes = "查询数据库中某个的学生信息")
    @ApiImplicitParam(name = "id", value = "学生ID", paramType = "path", required = true, dataType = "Integer")
    @GetMapping(value = "/u/{id}")
    public Account getStudent(@PathVariable String id) {
        logger.info("开始查询某个学生信息");
        return userDao.getById(Integer.valueOf(id));
    }

    /**
     * Create a new user with an auto-generated id and email and name as passed
     * values.
     */
    @PutMapping
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
    @DeleteMapping
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
    @PostMapping
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
    @PutMapping(value = "/update")
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

}
