package org.nakedobjects.object.control.defaults;

import org.nakedobjects.object.NakedObject;
import org.nakedobjects.object.control.ActionAbout;
import org.nakedobjects.object.control.State;
import org.nakedobjects.object.security.Role;
import org.nakedobjects.object.security.Session;
import org.nakedobjects.object.security.User;


/**
 * An About for contolling the action methods within a NakedObject.
 */
public class SimpleActionAbout extends AbstractAbout implements ActionAbout {
    private final static long serialVersionUID = 1L;

    public SimpleActionAbout(Session session, NakedObject object) {
        super(session, object);
    }

    public void changeNameIfUsable(String name) {
        if (canUse().isAllowed()) {
            setName(name);
        }
    }

    public void invisible() {
        super.invisible();
    }

    public void invisibleToUser(User user) {
        super.invisibleToUser(user);
    }

    public void invisibleToUsers(User[] users) {
        super.invisibleToUsers(users);
    }

    public void unusable() {
        super.unusable("Cannot be invoked");
    }

    public void unusable(String reason) {
        super.unusable(reason);
    }

    public void unusableInState(State state) {
        super.unusableInState(state);
    }

    public void unusableInStates(State[] states) {
        super.unusableInStates(states);
    }

    public void unusableOnCondition(boolean conditionMet, String reasonNotMet) {
        super.unusableOnCondition(conditionMet, reasonNotMet);
    }

    public void usableOnlyInState(State state) {
        super.usableOnlyInState(state);
    }

    public void usableOnlyInStates(State[] states) {
        super.usableOnlyInStates(states);
    }

    public void visibleOnlyToRole(Role role) {
        super.visibleOnlyToRole(role);
    }

    public void visibleOnlyToRoles(Role[] roles) {
        super.visibleOnlyToRoles(roles);
    }

    public void visibleOnlyToUser(User user) {
        super.visibleOnlyToUser(user);
    }

    public void visibleOnlyToUsers(User[] users) {
        super.visibleOnlyToUsers(users);
    }
}

/*
 * Naked Objects - a framework that exposes behaviourally complete business
 * objects directly to the user. Copyright (C) 2000 - 2003 Naked Objects Group
 * Ltd
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * The authors can be contacted via www.nakedobjects.org (the registered address
 * of Naked Objects Group is Kingsway House, 123 Goldworth Road, Woking GU21
 * 1NR, UK).
 */
