// umlrtcapsulepart.hh

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#include <stdlib.h>

#ifndef UMLRTCAPSULEPART_HH
#define UMLRTCAPSULEPART_HH

// An instance of this class is generated for each sub-capsule defined within a capsule type
// (i.e. UMLRTCapsuleRole) defined by the user.

// The UMLRTCapsuleRole + UMLRTCapsulePart instances define the tree-structure of the model
// and is used by the Frame Service to validate capsule instantiation.

#include "umlrtcapsuleclass.hh"
#include "umlrtcapsulerole.hh"

struct UMLRTSlot;

struct UMLRTCapsulePart
{
    const UMLRTCapsuleClass * containerClass;
    size_t roleIndex;

    size_t numSlot; // Replication factor.
    UMLRTSlot *  *  slots;

    size_t size ( ) const { return role() == NULL ? 0 : role()->multiplicityUpper; }

    const UMLRTCapsuleRole * role() const
    {
        return (containerClass->subcapsuleRoles == NULL) ? NULL : &containerClass->subcapsuleRoles[roleIndex];
    }
};

#endif // UMLRTCAPSULEPART_HH
