// ostime.hh

/*******************************************************************************
* Copyright (c) 2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#ifndef OSTIME_HH
#define OSTIME_HH

#include <time.h>

class OSTime
{
public:
    // needed for platform independent debug utilities
    static int get_timeofday(struct timeval * tv);
};

#ifdef NEED_CLOCK_GETTIME
typedef int clockid_t;
#define CLOCK_REALTIME 0
#define CLOCK_PROCESS_CPUTIME_ID 1
extern int clock_gettime (clockid_t __clock_id, struct timespec *__tp);
#endif

#endif // OSTIME_HH
