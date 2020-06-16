package org.constellation.dto.metadata;

import java.io.Serializable;
import org.constellation.dto.CstlUser;

/**
 * @author Mehdi Sidhoum (Geomatys).
 */
public class User extends CstlUser implements Serializable {

    private GroupBrief group;

    public User() {}

    public User(
            java.lang.Integer id,
            java.lang.String  login,
            java.lang.String  password,
            java.lang.String  firstname,
            java.lang.String  lastname,
            java.lang.String  email,
            java.lang.Boolean active,
            java.lang.String  avatar,
            java.lang.String  zip,
            java.lang.String  city,
            java.lang.String  country,
            java.lang.String  phone,
            java.lang.String  forgotPasswordUuid,
            java.lang.String  address,
            java.lang.String  additionalAddress,
            java.lang.String  civility,
            java.lang.String  title,
            java.lang.String  locale
        ) {
        super(id, login, password, firstname, lastname, email, active, avatar, zip, city, country, phone, forgotPasswordUuid, address, additionalAddress, civility, title, locale);
    }

    public User(CstlUser user) {
        super(user);
    }

    public GroupBrief getGroup() {
        return group;
    }

    public void setGroup(GroupBrief group) {
        this.group = group;
    }
}
