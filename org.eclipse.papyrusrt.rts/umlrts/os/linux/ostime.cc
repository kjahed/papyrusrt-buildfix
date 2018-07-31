// ostime.cc

/*******************************************************************************
 * Copyright (c) 2015 Zeligsoft (2009) Limited  and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

#include <sys/time.h>
#include <stddef.h>
#include "ostime.hh"

/*static*/int OSTime::get_timeofday(struct timeval * tv)
{
    return gettimeofday(tv, NULL);
}

#ifdef NEED_CLOCK_GETTIME
#ifndef __APPLE__
#error Unsupported platform configuration
#endif
#include <mach/clock.h>
#include <mach/mach.h>
#include <errno.h>

int clock_gettime (clockid_t __clock_id, struct timespec *__tp)
{
    int status;
    clock_serv_t clock;
    clock_id_t clock_id;
    mach_timespec_t mts;
    host_name_port_t self = mach_host_self();

    if (__clock_id == CLOCK_REALTIME)
        clock_id = REALTIME_CLOCK;
    else if (__clock_id == CLOCK_PROCESS_CPUTIME_ID)
        clock_id = SYSTEM_CLOCK;
    else
        return EINVAL;
    if (host_get_clock_service(self, clock_id, &clock) != KERN_SUCCESS)
        return EINVAL;
    status = clock_get_time(clock, &mts);
    mach_port_deallocate(self, clock);
    if (!status) {
        __tp->tv_sec = mts.tv_sec;
        __tp->tv_nsec = mts.tv_nsec;
    }
    return status;
}
#endif
