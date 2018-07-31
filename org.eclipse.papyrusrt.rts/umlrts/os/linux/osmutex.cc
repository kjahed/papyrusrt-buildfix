// osmutex.cc

/*******************************************************************************
 * Copyright (c) 2015 Zeligsoft (2009) Limited  and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

#include <pthread.h>
#include <errno.h>
#include <time.h>
#include "basefatal.hh"
#include "umlrtmutex.hh"
#include "umlrttimespec.hh"
#include "ostime.hh"

// platform-dependent implementation of mutual-exclusion.

UMLRTMutex::UMLRTMutex()
{
    mutex = new pthread_mutex_t;

    if (pthread_mutex_init((pthread_mutex_t *) mutex, NULL) != 0)
    {
        FATAL_ERRNO("pthread_mutex_init");
    }
}

// Can create a mutex which starts life as taken already.
UMLRTMutex::UMLRTMutex(bool taken)
{
    mutex = new pthread_mutex_t;

    if (pthread_mutex_init((pthread_mutex_t *) mutex, NULL) != 0)
    {
        FATAL_ERRNO("pthread_mutex_init");
    }

    if (taken)
    {
        take();
    }
}

UMLRTMutex::~UMLRTMutex()
{
    pthread_mutex_destroy((pthread_mutex_t *) mutex);
    delete (pthread_mutex_t *) mutex;
}
// Wait forever for mutex.
void UMLRTMutex::take()
{
    if (pthread_mutex_lock((pthread_mutex_t *) mutex) < 0)
    {
        FATAL_ERRNO("pthread_mutex_lock");
    }
}

#ifdef NEED_PTHREAD_MUTEX_TIMEDLOCK
int pthread_mutex_timedlock(pthread_mutex_t *mutex, const struct timespec *timeout)
{
    struct timespec start, mark, dwell;
    int retval;
  
    if (!pthread_mutex_trylock(mutex))
        return 0;

    dwell.tv_sec = 0;
    dwell.tv_nsec = 500 * 1000; // 0.5 ms.

    if (clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &start))
        return EINVAL;

    while ((retval = pthread_mutex_trylock(mutex)) == EBUSY) {
        if (clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &mark))
            return EINVAL;
        if ((mark.tv_sec - start.tv_sec) >= timeout->tv_sec &&
            (mark.tv_nsec - start.tv_nsec) >= timeout->tv_nsec) {
            return ETIMEDOUT;
        }
        nanosleep (&dwell, NULL);
    }
    return retval;
}
#endif

// Timed - returns non-zero for success, zero for timeout.
#if (((__GNUC__ * 100) + __GNUC_MINOR__) >= 406)
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wmaybe-uninitialized"
#endif

int UMLRTMutex::take(uint32_t msec)
{
    struct timespec timeout;
    int success = !0;
    int error;
    UMLRTTimespec::timespecAbsAddMsec(&timeout, msec);
    if ((error = pthread_mutex_timedlock((pthread_mutex_t *) mutex, &timeout)) != 0)
    {
        int errno_ = error;
        if (errno_ != ETIMEDOUT)
        {
            FATAL_ERRNO("pthread_mutex_timedlock");
        }
        success = 0;
    }
    return success;
}
#if (((__GNUC__ * 100) + __GNUC_MINOR__) >= 406)
#pragma GCC diagnostic pop
#endif

// Give mutex back.
void UMLRTMutex::give()
{
    if (pthread_mutex_unlock((pthread_mutex_t *) mutex) < 0)
    {
        FATAL_ERRNO("pthread_mutex_unlock");
    }
}

