// umlrtcontollercommand.hh

/*******************************************************************************
 * Copyright (c) 2015 Zeligsoft (2009) Limited  and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

#ifndef UMLRTCONTROLLERCOMMAND_HH
#define UMLRTCONTROLLERCOMMAND_HH

#include "umlrtqueueelement.hh"
#include <stdlib.h>

class UMLRTCapsule;
struct UMLRTSlot;

struct UMLRTControllerCommand
{
    typedef enum {
        UNDEFINED, // Not a command.
        ABORT, // Abort the controller.
        DEBUG_OUTPUT_MODEL, // Have controller thread output the model structure for debugging frame service.
        DEPORT, // Deport a capsule from a slot.
        DESTROY, // Destroy a slot.
        EXIT, // Normal exit of the controller.
        IMPORT, // Import a capsule into a slot.
        INCARNATE, // Incarnate a capsule into a slot i.e. initialize the dynamic capsule.
    } Command;

    UMLRTControllerCommand ( ) :
            command(UNDEFINED), capsule(NULL), isTopSlot(false), slot(NULL), userMsg(NULL), wait(NULL), exitValue(0) {}

    Command command; // All commands.

    UMLRTCapsule * capsule; // DEPORT, IMPORT, INCARNATE
    bool isTopSlot; // DESTROY
    UMLRTSignal signal; // INCARNATE
    UMLRTSlot * slot; // DEPORT, DESTROY, IMPORT
    const char * userMsg; // DEBUG_OUTPUT_MODEL
    UMLRTSemaphore * wait; // DESTROY
    void * exitValue; // ABORT, EXIT
/*
Command parameters:

ABORT
 - command
 - exitValue

DEBUG_OUTPUT_MODEL
 - command
 - userMsg (NULL permitted)

DEPORT
 - command
 - slot

DESTROY
 - command
 - slot
 - isTopSlot
 - wait

EXIT
 - command
 - exitValue

IMPORT
 - command
 - capsule
 - slot

INCARNATE
 - command
 - capsule
 - signal
*/

};

#endif // UMLRTCONTROLLERCOMMAND_HH
