// umlrttimerqueue.cc

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#include "basefatal.hh"
#include "basedebug.hh"
#include "umlrtapi.hh"
#include "umlrtguard.hh"
#include "umlrttimer.hh"
#include "umlrttimerqueue.hh"
#include "osnotify.hh"
#include <stdlib.h>

// Block time-sensitive operations during time adjustments.
UMLRTMutex UMLRTTimerQueue::timeAdjustMutex;

// Map of all timer-queues used during time-adjustments.
UMLRTHashMap * UMLRTTimerQueue::timerQueues;

// See umlrttimerqueue.hh for documentation.
UMLRTTimerQueue::UMLRTTimerQueue ( ) : notifyPtr(new UMLRTNotify())
{
    getTimerQueuesMap()->insert(this, this);
}

UMLRTTimerQueue::~UMLRTTimerQueue ( )
{
    getTimerQueuesMap()->remove(this);
    delete notifyPtr;
}

/*static*/ UMLRTHashMap * UMLRTTimerQueue::getTimerQueuesMap ( )
{
    if (!timerQueues)
    {
        timerQueues = new UMLRTHashMap("timerQueues", UMLRTHashMap::compareValue, false/*objectIsString*/);
    }
    return timerQueues;
}

/*static*/ void UMLRTTimerQueue::timeAdjustLock ( )
{
    timeAdjustMutex.take();
}

/*static*/ void UMLRTTimerQueue::timeAdjustUnlock ( )
{
    timeAdjustMutex.give();
}


/*static*/ void UMLRTTimerQueue::timeAdjustAllQueues ( const UMLRTTimespec & delta )
{
    // Assumes UMLRTTimerQueue::timeAdjustLock() was called to lock timeAdjustMutex.
    char tmbuf[UMLRTTimespec::TIMESPEC_TOSTRING_SZ];
    BDEBUG(BD_TIMER, "time adjustment delta %s\n", delta.toStringRelative(tmbuf, sizeof(tmbuf)));
    getTimerQueuesMap()->lock();
    UMLRTHashMap::Iterator iter = getTimerQueuesMap()->getIterator();

    while (iter != iter.end())
    {
        UMLRTTimerQueue * queue = (UMLRTTimerQueue *)iter.getObject();
        queue->timeAdjustElements(delta);
        iter = iter.next();
    }
    getTimerQueuesMap()->unlock();
}

void UMLRTTimerQueue::timeAdjustElements ( const UMLRTTimespec & delta )
{
    UMLRTGuard g(getMutex());

    UMLRTTimer * timer = (UMLRTTimer *)head;
    while (timer != NULL)
    {
        UMLRTTimespec was = timer->due;
        timer->due -= delta;
        char wasbuf[UMLRTTimespec::TIMESPEC_TOSTRING_SZ];
        char nowbuf[UMLRTTimespec::TIMESPEC_TOSTRING_SZ];
        BDEBUG(BD_TIMER, "timer adjust was %s now %s\n", was.toString(wasbuf, sizeof(wasbuf)), timer->due.toString(nowbuf, sizeof(nowbuf)));
        timer = (UMLRTTimer *)timer->next;
    }
}

// Remove the first timer on the queue. Returns NULL if first timer has not yet expired.
UMLRTTimer * UMLRTTimerQueue::dequeue ( )
{
    UMLRTGuard g(timeAdjustMutex, getMutex());

    UMLRTTimer * first = (UMLRTTimer *)head;

    if (first)
    {
        UMLRTTimespec now;
        UMLRTTimespec::getclock(&now);

        if (now >= first->due)
        {
            // First timer is due - dequeue it and return it.
            head = first->next;
            if (!_count)
            {
                FATAL("timer q(%p) dequeue found count zero first(%p) head(%p)\n", this, first, head);
            }
            --_count;
            if (head == NULL)
            {
                tail = NULL; // Not required, but cleaner.
            }
            char firstbuf[UMLRTTimespec::TIMESPEC_TOSTRING_SZ];
            char duebuf[UMLRTTimespec::TIMESPEC_TOSTRING_SZ];
            BDEBUG(BD_TIMER, "timer dequeue found now(%s) due(%s)\n", now.toString(firstbuf, sizeof(firstbuf)), first->due.toString(duebuf, sizeof(duebuf)));
        }
        else
        {
            // First timer still running - leave it there.
            first = NULL;
            BDEBUG(BD_TIMER, "this(%p) dequeue found first timer still running.\n", this);
        }
    }
    else
    {
        BDEBUG(BD_TIMER, "this(%p) dequeue found no timers.\n", this);
    }
    return first;
}

// Add a timer to the queue in the order of when they will expire.
void UMLRTTimerQueue::enqueue ( const UMLRTTimer * timer )
{
    UMLRTGuard g(timeAdjustMutex, getMutex());

    UMLRTTimer * next = (UMLRTTimer *)head;
    UMLRTTimer * previous = NULL;

    char tmbuf[UMLRTTimespec::TIMESPEC_TOSTRING_SZ];
    BDEBUG(BD_TIMER, "this(%p) timer-enqueue due(%s)\n", this, timer->due.toString(tmbuf,sizeof(tmbuf)));

    timer->next = NULL; // Initialize as last-in-queue.

    // Only need to notify the controller if the new timer ends up at the head of the queue.
    // The wait mechanism is only interested in the time remaining for the timer at the head of the queue.
    if (!head)
    {
        // List was empty. Put it in there as only element.
        head = tail = timer;
        _count = 1;
        notifyPtr->sendNotification();
    }
    else
    {
        // Skip ahead until we meet a timer due after this one.
        while (next && (next->due <= timer->due))
        {
            previous = next;
            next = (UMLRTTimer *)next->next;
        }
        if (!next)
        {
            // We're appending this timer to the end of the queue.
            tail->next = timer;
            tail = timer;
            ++_count;
        }
        else if (!previous)
        {
            // This timer is before the first element in the queue - prepend it.
            timer->next = head;
            head = timer;
            ++_count;
            notifyPtr->sendNotification();
        }
        else
        {
            // This timer goes after 'previous' and before 'next'.
            previous->next = timer;
            timer->next = next;
            ++_count;
        }
    }
}

// Calculate how much time left before timer on the head of the queue is due.
UMLRTTimespec UMLRTTimerQueue::timeRemaining ( ) const
{
    UMLRTGuard g(timeAdjustMutex, getMutex());

    // NOTE: Intended only for the consumer of the queue elements which has confirmed
    // the queue was non-empty. An alternate implementation is required if an empty queue
    // is possible.
    if (isEmpty())
    {
        FATAL("timer queue was empty in timeRemaining()");
    }
    UMLRTTimespec now;
    UMLRTTimespec::getclock(&now);

    BDEBUG(BD_TIMER, "this(%p) head(%p) tail(%p)\n", this, head, tail);

    UMLRTTimespec remain = ((UMLRTTimer*)head)->due - now;

    char tmbuf[UMLRTTimespec::TIMESPEC_TOSTRING_SZ];
    BDEBUG(BD_TIMER, "timeRemaining %s\n", remain.toStringRelative(tmbuf, sizeof(tmbuf)));

    return remain;
}

// Add a timer to the queue in the order of when they will expire.
bool UMLRTTimerQueue::cancel ( UMLRTTimerId id )
{
    UMLRTGuard g(getMutex());

    bool ok = false;
    UMLRTTimer * next = (UMLRTTimer *)head;
    UMLRTTimer * previous = NULL;

    while (next && (next != id.getTimer()))
    {
        previous = next;
        next = (UMLRTTimer *)next->next;
    }
    // Only need to notify the controller if the cancelled timer was at the head of the queue.
    if (next)
    {
        // Found the one to delete.
        if (!previous)
        {
            // This timer was at the head of the queue.
            head = (UMLRTTimer *)next->next;
            if (!_count)
            {
                FATAL("timer q(%p) count(%d) cancel at head(%p)\n", this, _count, head);
            }
            --_count;
            if (head == NULL)
            {
                tail = NULL; // Not strictly required, but cleaner.
            }
            notifyPtr->sendNotification();
        }
        else
        {
            // Unlink the timer being deallocated by setting next of previous to be
            // the cancelled timer's next.
            previous->next = next->next;
            if (!_count)
            {
                FATAL("timer q(%p) count(%d) cancel in middle timer(%p)\n", this, _count, next);
            }
            --_count;

            // If the timer was last in the queue, the tail has to be updated.
            if (tail == next)
            {
                tail = previous;
            }
        }
        // Return it to the pool.
        umlrt::TimerPutToPool(next);

        ok = true;
    }
    return ok;
}

int UMLRTTimerQueue::getNotifyFd ( )
{
    return notifyPtr->getNotifyFd();
}

// Clear notifications of pending commands.
void UMLRTTimerQueue::clearNotifyFd ( )
{
    notifyPtr->clearNotifyFd();
}

