// umlrtcommsportrole.hh

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#ifndef UMLRTCOMMSPORTROLE_HH
#define UMLRTCOMMSPORTROLE_HH

// This is the port information as defined by the model structure.

struct UMLRTCommsPortRole
{
    const int id;
    const char * const protocol;
    const char * const name;
    const char * const registrationOverride;
    const size_t numFarEnd;
    bool automatic; // True if the port should be registered as SAP/SPP at startup or during creation.
    bool conjugated; // Protocol is conjugated for this  port.
    bool locked; // True if the port registration should be locked.
    bool notification; // True if bind notification should be automatically enabled.
    bool sap; // True if the port is an SAP.
    bool spp; // True if the port is an SPP.
    bool wired; // True if the port is a wired port (as opposed to a service port).
};

#endif // UMLRTCOMMSPORTROLE_HH
