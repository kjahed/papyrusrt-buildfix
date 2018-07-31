// umlrtpriority.hh

/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

#ifndef UMLRTPRIORITY_HH
#define UMLRTPRIORITY_HH

enum
{
    PRIORITY_SYNCHRONOUS = 0,
    PRIORITY_SYSTEM,
    PRIORITY_PANIC,
    PRIORITY_HIGH,
    PRIORITY_NORMAL,
    PRIORITY_LOW,
    PRIORITY_BACKGROUND,
    PRIORITY_MAXPLUS1
};
typedef uint8_t UMLRTPriority;

#endif // UMLRTPRIORITY_HH
