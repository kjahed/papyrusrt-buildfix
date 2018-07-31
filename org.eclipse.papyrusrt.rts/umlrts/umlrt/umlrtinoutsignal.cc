// umlrtinoutsignal.hh

/*******************************************************************************
* Copyright (c) 2017 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#include "umlrtinoutsignal.hh"
#include "basefatal.hh"

UMLRTInOutSignal::UMLRTInOutSignal()
{

}

UMLRTInOutSignal::UMLRTInOutSignal(const UMLRTInOutSignal &signal) : UMLRTSignal(signal)
{

}

UMLRTInOutSignal& UMLRTInOutSignal::operator=(const UMLRTInOutSignal &signal)
{
    UMLRTSignal::operator = (signal);
    return *this;
}

UMLRTInOutSignal::~UMLRTInOutSignal()
{

}


