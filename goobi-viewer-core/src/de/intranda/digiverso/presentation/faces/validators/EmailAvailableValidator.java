/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.faces.validators;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.user.User;

/**
 * Duplicate validator for e-mail addresses.
 */
@FacesValidator("emailAvailableValidator")
public class EmailAvailableValidator implements Validator<String> {

    /* (non-Javadoc)
     * @see javax.faces.validator.Validator#validate(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
     */
    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        try {
            if (!validateEmailUniqueness(value)) {
                FacesMessage msg = new FacesMessage(Helper.getTranslation("email_errExists", null), "");
                msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(msg);
            }
        } catch (DAOException e) {
            FacesMessage msg = new FacesMessage(Helper.getTranslation("login_internalerror", null), "");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    /**
     * @param email
     * @return  true if the given email is not already assigned to a user except a possibly currently logged in user in this session
     * @throws DAOException 
     */
    private static boolean validateEmailUniqueness(String email) throws DAOException {
        User user = DataManager.getInstance().getDao().getUserByEmail(email);
        User currentUser = BeanUtils.getUserBean().getUser();
        if(user != null) {
            return currentUser != null && user.getId().equals(currentUser.getId());
        }
        
        return true;
    }
}
