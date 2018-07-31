// ossemaphore.cc

/*******************************************************************************
* Copyright (c) 2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#include <semaphore.h>
#include <errno.h>
#include <time.h>
#include <stdio.h>
#include "basefatal.hh"
#include "umlrtsemaphore.hh"
#include "umlrttimespec.hh"

UMLRTSemaphore::UMLRTSemaphore( int value )
{
    sem = new sem_t;

    if (sem_init((sem_t *) sem, 0/*threads only*/, value) < 0)
    {
        FATAL_ERRNO("sem_init");
    }
}

UMLRTSemaphore::~UMLRTSemaphore()
{
    if (sem_destroy((sem_t *) sem) < 0)
    {
        FATAL_ERRNO("sem_destroy");
    }

    delete (sem_t *) sem;
}

// Wait forever for semaphore.
void UMLRTSemaphore::wait()
{
    if (sem_wait((sem_t *) sem) < 0)
    {
        FATAL_ERRNO("sem_wait");
    }
}

#ifdef NEED_SEM_TIMEDWAIT
#include "ostime.hh"
int sem_timedwait(sem_t *sem, const struct timespec *timeout)
{
    struct timespec start, mark, dwell;
    int retval;
    int status;
   
    if (!sem_trywait(sem))
        return 0;

    dwell.tv_sec = 0;
    dwell.tv_nsec = 500 * 1000; // 0.5 ms.

    if (clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &start))
        return EINVAL;

    while ((retval = sem_trywait(sem)) == EAGAIN) {
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
int UMLRTSemaphore::wait( uint32_t msec )
{
    struct timespec timeout;
    int success = !0;

    UMLRTTimespec::timespecAbsAddMsec( &timeout, msec );
    if (sem_timedwait((sem_t *) sem, &timeout) < 0)
    {
#if (((__GNUC__ * 100) + __GNUC_MINOR__) >= 406)
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wmaybe-uninitialized"
#endif
        int errno_ = errno;
#if (((__GNUC__ * 100) + __GNUC_MINOR__) >= 406)
#pragma GCC diagnostic pop
#endif
        if (errno_ != ETIMEDOUT)
        {
            perror("UMLRTSemaphore wait");
            FATAL("sem_timedwait (had waited %d msec)", msec);
        }
        success = 0;
    }
    return success;
}

// Give semaphore back.
void UMLRTSemaphore::post()
{
    if (sem_post((sem_t *) sem) < 0)
    {
        FATAL_ERRNO("sem_wait");
    }
}
