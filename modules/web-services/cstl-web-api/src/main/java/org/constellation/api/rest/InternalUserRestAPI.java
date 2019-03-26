/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.constellation.api.rest;

import java.util.Optional;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import static org.constellation.api.rest.AbstractRestAPI.LOGGER;
import org.constellation.dto.UserWithRole;
import org.constellation.dto.CstlUser;
import org.geotoolkit.util.StringUtilities;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RestController()
public class InternalUserRestAPI extends AbstractRestAPI {

    /**
     * This method is to delete.
     *
     * replace the call to this api by UserRestAPI.getUser(currentID)
     */
    @RequestMapping(value = "/internal/users/current", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity currentUser(final HttpServletRequest req) {
        try {
            final int userId = assertAuthentificated(req);
            final Optional<UserWithRole> optUser = userRepository.findOneWithRole(userId);
            if (optUser.isPresent()) {
                return new ResponseEntity(optUser.get(), OK);
            } else {
                return new ErrorMessage(NOT_FOUND).i18N(I18nCodes.User.NOT_FOUND).build();
            }
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * This method is to delete.
     *
     * replace the call to this api by UserRestAPI.updateUser(currentID)
     */
    @RequestMapping(value = "/internal/users/current", method = POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity myAccount(@RequestParam(name = "userId") Integer userId,
            @RequestParam(name = "login") String login,
            @RequestParam(name = "email") String email,
            @RequestParam(name = "lastname") String lastname,
            @RequestParam(name = "firstname") String firstname,
            @RequestParam(name = "address", required = false) String address,
            @RequestParam(name = "additionalAddress", required = false) String additionalAddress,
            @RequestParam(name = "zip", required = false) String zip,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "country", required = false) String country,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(name = "group", required = false) Integer group,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "locale", required = false) String locale,
            final HttpServletRequest req) {
        try {
            final int currentUserId = assertAuthentificated(req);
            Optional<CstlUser> optionalUser = userRepository.findById(currentUserId);
            if (optionalUser.isPresent()) {
                CstlUser user = optionalUser.get();
                user.setLogin(login);
                user.setFirstname(firstname);
                user.setLastname(lastname);
                user.setEmail(email);
                user.setAddress(address);
                user.setAdditionalAddress(additionalAddress);
                user.setZip(zip);
                user.setCity(city);
                user.setCountry(country);
                user.setPhone(phone);
                user.setLocale(locale);

                //check password update
                String newPassword = StringUtilities.MD5encode(password);
                if (password != null
                        && !password.isEmpty()
                        && !newPassword.equals(user.getPassword())) {
                    user.setPassword(newPassword);
                }

                userRepository.update(user);
                return new ResponseEntity(OK);
            }
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
        return new ErrorMessage(NOT_FOUND).i18N(I18nCodes.User.NOT_FOUND).build();
    }

    /**
     * This method is to delete.
     *
     * replace the call to this api by UserRestAPI.updateUser
     */
    @RequestMapping(value = "/internal/users", method = POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity editUser(@RequestParam(name = "userId") Integer userId,
            @RequestParam(name = "login") String login,
            @RequestParam(name = "email") String email,
            @RequestParam(name = "lastname") String lastname,
            @RequestParam(name = "firstname") String firstname,
            @RequestParam(name = "address", required = false) String address,
            @RequestParam(name = "additionalAddress", required = false) String additionalAddress,
            @RequestParam(name = "zip", required = false) String zip,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "country", required = false) String country,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(name = "group", required = false) Integer group,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "locale", required = false) String locale) {
        Optional<CstlUser> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            CstlUser user = optionalUser.get();
            user.setLogin(login);
            user.setFirstname(firstname);
            user.setLastname(lastname);
            user.setEmail(email);
            user.setAddress(address);
            user.setAdditionalAddress(additionalAddress);
            user.setZip(zip);
            user.setCity(city);
            user.setCountry(country);
            user.setPhone(phone);
            user.setLocale(locale);

            //check password update
            String newPassword = StringUtilities.MD5encode(password);
            if (password != null
                    && !password.isEmpty()
                    && !newPassword.equals(user.getPassword())) {
                user.setPassword(newPassword);
            }

            userRepository.update(user);

            //add user to role
            userRepository.addUserToRole(user.getId(), role);

            return new ResponseEntity(OK);
        }
        return new ErrorMessage(NOT_FOUND).i18N(I18nCodes.User.NOT_FOUND).build();
    }

    /**
     * This method is to delete it should be a PUT but the current MULTI-PART SPRING resolver does not support POST if not overriden.
     *
     * replace the call to this api by UserRestAPI.createUser
     */
    @RequestMapping(value = "/internal/users/add", method = POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity addUser(
            @RequestParam(name = "login") String login,
            @RequestParam(name = "email") String email,
            @RequestParam(name = "lastname") String lastname,
            @RequestParam(name = "firstname") String firstname,
            @RequestParam(name = "address", required = false) String address,
            @RequestParam(name = "additionalAddress", required = false) String additionalAddress,
            @RequestParam(name = "zip", required = false) String zip,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "country", required = false) String country,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "locale", required = false) String locale) {

        //add user
        CstlUser user = new CstlUser();
        user.setId(null);
        user.setLogin(login);
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setEmail(email);
        user.setActive(true);
        user.setAddress(address);
        user.setAdditionalAddress(additionalAddress);
        user.setZip(zip);
        user.setCity(city);
        user.setCountry(country);
        user.setPhone(phone);
        user.setPassword(StringUtilities.MD5encode(password));
        user.setLocale(locale);

        user = userRepository.create(user);

        //add user to role
        userRepository.addUserToRole(user.getId(), role);

        return new ResponseEntity(OK);
    }
}
