// umlrtprioritymessagequeue.hh

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#ifndef UMLRTPRIORITYMESSAGEQUEUE_HH
#define UMLRTPRIORITYMESSAGEQUEUE_HH

#include "umlrtmessagequeue.hh"
#include "umlrtsemaphore.hh"
#include "umlrtsignal.hh"
#include "umlrttimerqueue.hh"
#include "umlrtpriority.hh"

class UMLRTMessage;
class UMLRTNotify;

// UMLRTPriorityMessageQueue - a collection of priority-message queues.

// This object has one message queue per priority level.

// The priority of each message is contained in the signal (within the message).

class UMLRTPriorityMessageQueue
{
public:

    UMLRTPriorityMessageQueue ( const char * owner );

    virtual ~UMLRTPriorityMessageQueue ( );

    // Return the queue associated with a priority.
    UMLRTMessageQueue & getQueue ( UMLRTPriority priority );

    // Used to transfer all messages from each priority queue to
    // their associated priority-queue

    void moveAll ( UMLRTPriorityMessageQueue & fromQueue );

    // Queue up all messages associated with timed-out timers.
    void queueTimerMessages ( UMLRTTimerQueue * timerQueue );

    // Get the highest priority message from the collection of queues.
    UMLRTMessage * dequeueHighestPriority ( );

    // Put the message into its appropriate priority-queue.
    void enqueue ( UMLRTMessage * msg, bool front = false );

    // NOTE: isEmpty() is intended only for the sole consumer of the queue and
    // only reflects the emptiness of the queue at the time of calling.
    // Synchronization (i.e. the 'notify' mechanism) is required to ensure the
    // consumer will re-visit the queue when an item is queued.
    bool isEmpty ( );

    // Return the current # of queued messages.
    // NOTE: count() is intended only for the sole consumer of the queue. See isEmpty() comments.
    size_t count ( );

    // Purge queue of elements that match the criteria.
    void remove ( UMLRTQueue::match_compare_t callback, UMLRTQueue::match_notify_t notify, void * userData );

    // return umlrtnotify fd[READ]
    int getNotifyFd();

    // Clear notifications of pending commands.
    void clearNotifyFd();

private:
    UMLRTPriorityMessageQueue();

    // Owner - for debugging.
    const char * const owner;

    // One message queue per priority.
    UMLRTMessageQueue queue[PRIORITY_MAXPLUS1];

    UMLRTNotify * notifyPtr;
};

#endif // UMLRTPRIORITYMESSAGEQUEUE_HH
