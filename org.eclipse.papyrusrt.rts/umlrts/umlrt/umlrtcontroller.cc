// umlrtcontroller.cc

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

// UMLRTController is the main controller-class.

#include "basedebug.hh"
#include "basefatal.hh"
#include "umlrtapi.hh"
#include "umlrtcontroller.hh"
#include "umlrtcapsule.hh"
#include "umlrtslot.hh"
#include "umlrtcapsuletocontrollermap.hh"
#include "umlrtcapsulerole.hh"
#include "umlrtcapsulepart.hh"
#include "umlrtcommsport.hh"
#include "umlrtcommsportfarend.hh"
#include "umlrtcommsportrole.hh"
#include "umlrtcontrollercommand.hh"
#include "umlrtframeservice.hh"
#include "umlrtobjectclass.hh"
#include "umlrtpriority.hh"
#include "umlrtprotocol.hh"
#include "umlrtsignal.hh"
#include "umlrttimer.hh"
#include "umlrttimespec.hh"
#include "umlrtqueue.hh"
#include <stdlib.h>
#include <stdio.h>
#include "osselect.hh"
#include <string.h>
#include <stdarg.h>
#include <new>

// The application-wide free message pool.
/*static*/ UMLRTMessagePool * UMLRTController::messagePool = NULL;

// The application-wide free signal pool.
/*static*/ UMLRTSignalElementPool * UMLRTController::signalElementPool  = NULL;

// The application-wide free timer pool.
/*static*/ UMLRTTimerPool * UMLRTController::timerPool  = NULL;

// Error codes to string
static const char * errorToString[] = UMLRTCONTROLLER_ERROR_CODE_TO_STRING;


UMLRTController::UMLRTController (const char * name__, size_t numSlots_, UMLRTSlot slots_[] )
    : UMLRTBasicThread(name__), name_(name__), incomingQueue(name__), capsuleQueue(name__), numSlots(numSlots_), slots(slots_), _exit(false), exitValue(0), _abort(false), lastError(E_OK)
{
    // Register the controller with the capsule-to-controller map.
    UMLRTCapsuleToControllerMap::addController(name__, this);
}

UMLRTController::UMLRTController ( const char * name__ )
    : UMLRTBasicThread(name__), name_(name__), incomingQueue(name__), capsuleQueue(name__), numSlots(0), slots(NULL), _exit(false), exitValue(0), _abort(false), lastError(E_OK)
{
    // Register the controller with the capsule-to-controller map.
    UMLRTCapsuleToControllerMap::addController(name__, this);
}

bool UMLRTController::cancelTimer ( const UMLRTTimerId id )
{
    if (id.isValid())
    {
        const UMLRTTimer * timer = id.getTimer();

        char buf[UMLRTTimespec::TIMESPEC_TOSTRING_SZ];
        BDEBUG(BD_TIMER, "cancel timer(%p) destPort(%s) isInterval(%d) priority(%d) payloadSize(%d) due(%s)\n",
                timer, timer->sap()->getName(), timer->isInterval, timer->signal.getPriority(), timer->signal.getPayloadSize(),
                timer->isInterval ? timer->due.toStringRelative(buf, sizeof(buf)) : timer->due.toString(buf, sizeof(buf)));
    }
    return timerQueue.cancel(id);
}

/*static*/ bool UMLRTController::deallocateMsgMatchCompare ( UMLRTMessage * msg, t_message_match_criteria * criteria )
{
    bool match = (!msg->isCommand && msg->destSlot == criteria->slot);
    if ((!match) && msg->isCommand)
    {
        UMLRTControllerCommand * command = (UMLRTControllerCommand *)msg->signal.getPayload();

        if (command != NULL)
        {
            switch (command->command)
            {
            case UMLRTControllerCommand::ABORT:
            case UMLRTControllerCommand::DEBUG_OUTPUT_MODEL:
                // Not a match.
                break;
            case UMLRTControllerCommand::DESTROY:
                // Leave the destroy in the queue if this is a deport-deallocate.
                match = criteria->isDestroy && (command->slot == criteria->slot);
                break;
            case UMLRTControllerCommand::IMPORT:
            case UMLRTControllerCommand::DEPORT:
                if (criteria->isDestroy)
                {
                    // If this is destroy-deallocate, get rid of all IMPORTs/DEPORTs regardless.
                    match = criteria->isDestroy && (command->slot == criteria->slot);
                }
                else
                {
                    // else this is deport-deallocate, allow an IMPORT/DEPORT of another capsule.
                    match = (command->slot == criteria->slot) && (command->capsule == criteria->capsule);
                }
                break;
            case UMLRTControllerCommand::INCARNATE:
                // Deallocate INCARNATE if this is destroy-deallocate.
                match = criteria->isDestroy && (command->capsule == criteria->capsule);
                break;
            case UMLRTControllerCommand::UNDEFINED:
            default:
                FATAL("unknown controller command (%d) being matched for deallocate", command->command);
            }
            if (match)
            {
                BDEBUG(BD_CONTROLLER, "deallocate controller command(%d) for slot %p capsule %p\n",
                        command->command, criteria->slot, criteria->capsule);
            }
        }
    }
   return match;
}

/*static*/ void UMLRTController::deallocateMsgMatchNotify ( UMLRTMessage * msg, UMLRTSlot * slot )
{
    BDEBUG(BD_DESTROY, "Purging message destined for slot %p\n", slot);

    umlrt::MessagePutToPool(msg);
}

// Callback to purge timers for a condemned slot.
/*static*/ bool UMLRTController::deallocateTimerMatchCompare ( UMLRTTimer * timer, UMLRTSlot * slot )
{
    return timer->destSlot == slot;
}

// Callback when a timer is being deleted.
/*static*/ void UMLRTController::deallocateTimerMatchNotify ( UMLRTTimer * timer, UMLRTSlot * slot )
{
    BDEBUG(BD_DESTROY, "Purging timer destined for slot %s\n", slot->name);

    umlrt::TimerPutToPool(timer);
}

void UMLRTController::deallocateSlotResources ( UMLRTSlot * slot, UMLRTCapsule * deletedCapsule, bool isDestroy )
{
    if (slot == NULL)
    {
        FATAL("attempting to deallocate the NULL slot.");
    }
    BDEBUG(BD_DESTROY, "Remove messages and timers for slot %s\n", slot->name);

    t_message_match_criteria criteria = { slot, deletedCapsule, isDestroy };

    incomingQueue.remove( (UMLRTQueue::match_compare_t)deallocateMsgMatchCompare, (UMLRTQueue::match_notify_t)deallocateMsgMatchNotify, &criteria );
    capsuleQueue.remove( (UMLRTQueue::match_compare_t)deallocateMsgMatchCompare, (UMLRTQueue::match_notify_t)deallocateMsgMatchNotify, &criteria );
    timerQueue.remove( (UMLRTQueue::match_compare_t)deallocateTimerMatchCompare, (UMLRTQueue::match_notify_t)deallocateTimerMatchNotify, slot );
}

// Deliver a signal to the destination port.
bool UMLRTController::deliver ( const UMLRTCommsPort * destPort, const UMLRTSignal &signal, size_t srcPortIndex )
{
    // Assumes global RTS lock acquired.

    UMLRTMessage * msg = umlrt::MessageGetFromPool();
    bool ok = false;

    if (!msg)
    {
        signal.getSrcPort()->slot->controller->setError(E_SEND_NO_MSG_AVL);
    }
    else if (destPort == NULL)
    {
        FATAL("Message from slot %s (port %s) has destination port NULL.", signal.getSrcPort()->slotName(), signal.getSrcPort()->getName());
    }
    else
    {
        // Look up sapIndex0 so receiver knows which index in their replicated port the message was received on.
        msg->sapIndex0_ = signal.getSrcPort()->farEnds[srcPortIndex].farEndIndex;
        msg->signal = signal;
        msg->destPort = destPort;
        msg->destSlot = destPort->slot;
        msg->srcPortIndex = srcPortIndex;
        msg->isCommand = false;

        // Source port may not exist.
        BDEBUG(BD_SIGNALALLOC, "%s: deliver signal-qid[%d] id(%d) -> %s(%s[%d]) payloadSize(%d)\n",
                name(),
                msg->signal.getQid(),
                msg->signal.getId(),
                msg->sap()->slotName(), msg->sap()->getName(), msg->sapIndex0(),
                msg->signal.getPayloadSize());

        if (isMyThread())
        {
            // If this is me delivering the message, I can deliver directly to my capsule queues.
            capsuleQueue.enqueue(msg);
        }
        else
        {
            // Otherwise, I deliver to the remote capsule's incoming queue.
            incomingQueue.enqueue(msg);
        }
        ok = true;
    }
    return ok;
}

void UMLRTController::enqueueAbort ( )
{
    UMLRTControllerCommand command;

    // Explicitly reset unused command contents.
    command.capsule = NULL;
    command.isTopSlot = false;
    command.slot = NULL;
    command.userMsg = NULL;
    command.wait = NULL;

    // Format command and enqueue it.
    command.command = UMLRTControllerCommand::ABORT;
    command.exitValue = (void *)EXIT_FAILURE;
    enqueueCommand(command);
}

void UMLRTController::enqueueAbortAllControllers ( )
{
    // The original controller doing the abort-all outputs the model.
    debugOutputModel("User-initiated abort");
    UMLRTCapsuleToControllerMap::enqueueAbortAllControllers();
}

void UMLRTController::enqueueCommand ( const UMLRTControllerCommand & command )
{
    UMLRTMessage *msg = umlrt::MessageGetFromPool();

    msg->signal.initialize("ControllerCommand", UMLRTSignal::invalidSignalId, sizeof(UMLRTControllerCommand));
    UMLRTControllerCommand * msgcmd;

    BDEBUG(BD_SIGNALALLOC, "%s: deliver signal-qid[%d] as command %d\n",
            name(),
            msg->signal.getQid(),
            command.command);

    if ((msgcmd = (UMLRTControllerCommand *)msg->signal.getPayload()) == NULL)
    {
        FATAL("initialized signal had no payload.");
    }
    new(msgcmd) UMLRTControllerCommand(command);

    msg->isCommand = true;

    if (isMyThread())
    {
        // If this is me delivering the message, I can deliver directly to my capsule queues.
        capsuleQueue.enqueue(msg);
    }
    else
    {
        // Otherwise, I deliver to the remote capsule's incoming queue.
        incomingQueue.enqueue(msg);
    }
}

void UMLRTController::enqueueDebugOutputModel ( const char * userMsg )
{
    UMLRTControllerCommand command;

    // Explicitly reset unused command contents.
    command.capsule = NULL;
    command.isTopSlot = false;
    command.slot = NULL;
    command.wait = NULL;
    command.exitValue = NULL;

    // Format command and enqueue it.
    command.command = UMLRTControllerCommand::DEBUG_OUTPUT_MODEL;
    command.userMsg = (userMsg == NULL) ? NULL : strdup(userMsg);

    enqueueCommand(command);
}

void UMLRTController::enqueueDeport ( UMLRTSlot * slot, UMLRTCapsule * capsule )
{
    UMLRTControllerCommand command;

    // Explicitly reset unused command contents.
    command.isTopSlot = false;
    command.userMsg = NULL;
    command.wait = NULL;
    command.exitValue = NULL;

    // Format command and enqueue it.
    command.command = UMLRTControllerCommand::DEPORT;
    command.slot = slot;
    command.capsule = capsule;

    enqueueCommand(command);
}

void UMLRTController::enqueueDestroy ( UMLRTSlot * slot, bool isTopSlot )
{
    UMLRTControllerCommand command;

    // Explicitly reset unused command contents.
    command.capsule = NULL;
    command.userMsg = NULL;
    command.exitValue = NULL;

    // Format command and enqueue it.
    command.command = UMLRTControllerCommand::DESTROY;
    command.slot = slot;
    command.isTopSlot = isTopSlot;
    command.wait = NULL;
    if (isTopSlot)
    {
        command.wait = new UMLRTSemaphore(0);
    }
    enqueueCommand(command);
    if (isTopSlot)
    {
        BDEBUG(BD_DESTROY, "Waiting for controller '%s' to destroy slot '%s'.\n", getName(), slot->name);
        command.wait->wait();
        BDEBUG(BD_DESTROY, "Controller '%s' destroyed slot '%s'.\n", getName(), slot->name);
        delete command.wait;
    }
}

void UMLRTController::enqueueExit ( void * exitValue )
{
    UMLRTControllerCommand command;

    // Explicitly reset unused command contents.
    command.capsule = NULL;
    command.isTopSlot = false;
    command.slot = NULL;
    command.userMsg = NULL;
    command.wait = NULL;

    // Format command and enqueue it.
    command.command = UMLRTControllerCommand::EXIT;
    command.exitValue = exitValue;
    enqueueCommand(command);
}

void UMLRTController::enqueueExitAllControllers ( void * exitValue )
{
    UMLRTCapsuleToControllerMap::enqueueExitAllControllers(exitValue);
}

void UMLRTController::enqueueImport ( UMLRTSlot * slot, UMLRTCapsule * capsule )
{
    UMLRTControllerCommand command;

    // Explicitly reset unused command contents.
    command.isTopSlot = false;
    command.userMsg = NULL;
    command.wait = NULL;
    command.exitValue = NULL;

    // Format command and enqueue it.
    command.command = UMLRTControllerCommand::IMPORT;
    command.slot = slot;
    command.capsule = capsule;

    enqueueCommand(command);
}

void UMLRTController::enqueueIncarnate ( UMLRTCapsule * capsule, const UMLRTCommsPort * srcPort, const UMLRTObject_class * type, const void * userData )
{
    UMLRTControllerCommand command;

    // May be called from within the context of another controller thread.

    // Explicitly reset unused command contents.
    command.isTopSlot = false;
    command.slot = NULL;
    command.userMsg = NULL;
    command.wait = NULL;
    command.exitValue = NULL;

    if (type != NULL)
    {
        command.signal.initialize("INCARNATE", UMLRTSignal::invalidSignalId, srcPort, type, userData);
    }
    else
    {
        command.signal.initialize("INCARNATE", UMLRTSignal::invalidSignalId, srcPort);
    }
    // Format command and enqueue it.
    command.command = UMLRTControllerCommand::INCARNATE;
    command.capsule = capsule;

    // Controller frees serialized data buffer back to the heap.

    // Notification to destination controller occurs as a result of this enqueue.
    enqueueCommand(command);
}

// Return true if abort was received.
void UMLRTController::executeCommand ( UMLRTMessage * msg )
{
    UMLRTControllerCommand * command = (UMLRTControllerCommand *)msg->signal.getPayload();

    if (command != NULL)
    {
        switch (command->command)
        {
        case UMLRTControllerCommand::ABORT:
            BDEBUG(BD_COMMAND, "%s: ABORT command received\n", name());
            _abort = true;
            exitValue = command->exitValue;
            break;

        case UMLRTControllerCommand::DEBUG_OUTPUT_MODEL:
            BDEBUG(BD_COMMAND, "%s: DEBUG_OUTPUT_MODEL command received\n", name());
            debugOutputModel(command->userMsg);
            if (command->userMsg != NULL)
            {
                free((void*)command->userMsg);
            }
            break;

        case UMLRTControllerCommand::DEPORT:
            BDEBUG(BD_COMMAND, "%s: DEPORT from slot %s command received\n", name(), command->slot->name);
            UMLRTFrameService::controllerDeport(command->slot, false/*synchronous*/, false/*lockAcquired*/);
            break;

        case UMLRTControllerCommand::DESTROY:
            BDEBUG(BD_COMMAND, "%s: DESTROY from slot %s (is %stop slot) command received\n", name(), command->slot->name, command->isTopSlot ? "" : "NOT ");
            UMLRTFrameService::controllerDestroy(command->slot, command->isTopSlot, false/*synchronous*/, false/*lockAcquired*/);
            if (command->isTopSlot && (command->wait != NULL))
            {
                BDEBUG(BD_DESTROY, "Controller '%s' destroyed top slot '%s'\n", getName(), command->slot->name);
                command->wait->post();
            }
            break;

        case UMLRTControllerCommand::EXIT:
            BDEBUG(BD_COMMAND, "%s: EXIT command received\n", name());
            _exit = true;
            exitValue = command->exitValue;
            break;

        case UMLRTControllerCommand::IMPORT:
            BDEBUG(BD_COMMAND, "%s: IMPORT capsule %s to slot %s command received\n", name(), command->capsule->name(), command->slot->name);
            UMLRTFrameService::controllerImport(command->slot, command->capsule, false/*synchronous*/, false/*lockAcquired*/);
            break;

        case UMLRTControllerCommand::INCARNATE:
            BDEBUG(BD_COMMAND, "%s: INCARNATE capsule %s command received\n", name(), command->capsule->name());
            UMLRTFrameService::controllerIncarnate(command->capsule, command->signal);
            break;

        case UMLRTControllerCommand::UNDEFINED:
        default:
            FATAL("%s:unknown controller (%d) command received", name(), command->command);
        }
    }
}

// Initialize the free pools.
/*static*/ void UMLRTController::initializePools ( UMLRTSignalElementPool * signalElementPool_, UMLRTMessagePool * messagePool_,
        UMLRTTimerPool * timerPool_ )
{
    signalElementPool = signalElementPool_;
    messagePool = messagePool_;
    timerPool = timerPool_;
}

// Wait for the controller thread to die.
void * UMLRTController::join ( )
{
    return UMLRTBasicThread::join();
}

// Get the system-wide signal pool.
/*static*/ UMLRTSignalElementPool * UMLRTController::getSignalElementPool ( )
{
    return signalElementPool;
}

// Get the system-wide message pool.
/*static*/ UMLRTMessagePool * UMLRTController::getMessagePool ( )
{
    return messagePool;
}

// Get the system-wide timer pool.
/*static*/ UMLRTTimerPool * UMLRTController::getTimerPool ( )
{
    return timerPool;
}

bool UMLRTController::isMyThread ( )
{
    return UMLRTBasicThread::isMyThread();
}

// Output an error containing a user-defined message and the 'strerror()' string.
void UMLRTController::perror ( const char * fmt, ... ) const
{
    va_list ap;
    va_start(ap, fmt);
    vprintf(fmt, ap);
    printf(": %s\n", strerror());
    va_end(ap);
}

// Deliver a signal to the destination port.
void UMLRTController::recall ( UMLRTMessage * msg, bool front )
{
    capsuleQueue.enqueue(msg, front);
}

// Main loop
void * UMLRTController::run ( void * args )
{
    printf("Controller \"%s\" running.\n", name());

    if (slots == NULL)
    {
        numSlots = UMLRTCapsuleToControllerMap::getDefaultSlotList( &slots );
    }
    for (size_t i = 0; (i < numSlots) && (slots != NULL); ++i)
    {
        if (slots[i].controller == this)
        {
            if ((slots[i].capsule != NULL) && (!slots[i].condemned))
            {
                if (   (slots[i].role() == NULL)
                    || ((slots[i].role()->optional == 0) && (slots[i].role()->plugin == 0)))
                {
                    BDEBUG(BD_INSTANTIATE, "%s: initialize capsule %s (class %s).\n", name(), slots[i].name, slots[i].capsuleClass->name );
                    UMLRTSignal emptySignal;
                    emptySignal.initialize("INITIALIZE", UMLRTSignal::invalidSignalId);
                    UMLRTFrameService::initializeCapsule(slots[i].capsule, emptySignal);
                }
            }
        }
    }
    while (!_abort && !_exit)
    {
        // Queue messages associated with all timed-out timers.
        capsuleQueue.queueTimerMessages(&timerQueue);

        // Transfer all incoming messages to capsule queues.
        capsuleQueue.moveAll(incomingQueue);

        // Inject all available messages, highest priority msgs first.
        UMLRTMessage * msg;

        // Process only as many capsuleQueue messages as is currently queued before exiting the inner loop and checking the incomingQueue again.
        // If additional capsuleQueue messages are queued while injecting these, they will be processed after the incomingQueue is checked.
        size_t countBeforeInnerLoop = capsuleQueue.count();
        size_t innerLoopCount = 0;

        while (!_exit && !_abort && (innerLoopCount < countBeforeInnerLoop) && ((msg = capsuleQueue.dequeueHighestPriority()) != NULL))
        {
            ++innerLoopCount;

            if (msg->isCommand)
            {
                executeCommand(msg);
            }
            else if (msg->destSlot->capsule == NULL)
            {
                FATAL("%s: signal id(%d) to slot %s (no capsule instance) should not occur\n",
                        name(), msg->signal.getId(), msg->destSlot->name);
            }
            else
            {
                if (msg->destSlot->condemned)
                {
                    // Drop messages to a condemned slot.
                    BDEBUG(BD_INJECT, "%s: dropping signal-qid[%d] id(%d)(%s) to slot %s (slot condemned)\n",
                            name(), msg->signal.getQid(), msg->getSignalId(), msg->getSignalName(), msg->sap()->getName());
                }
                else
                {
                    if (base::debugTypeEnabled(BD_INJECT))
                    {
                        BDEBUG(BD_INJECT, "%s: countBeforeInnerLoop(%d) innerLoopCount(%d) signal(%s)\n",
                                name(), countBeforeInnerLoop, innerLoopCount, msg->getSignalName());
                        // Source port may no longer exist.
                        BDEBUG(BD_INJECT, "%s: inject signal-qid[%d] into %s(role %s, class %s) {%s[%d]} id %d(%s) prio(%d)\n",
                                name(), msg->signal.getQid(), msg->destSlot->capsule->name(), msg->destSlot->capsule->getName(),
                                msg->destSlot->capsule->getTypeName(), msg->sap()->getName(), msg->sapIndex0(), msg->signal.getId(),
                                msg->getSignalName(), msg->getPriority());
                        size_t param_i = 0;
                        const UMLRTObject_class * type = msg->getType(param_i++);
                        while (type != NULL)
                        {
                            BDEBUG(BD_INJECT, "%s: signal %s param[%d] type %s\n", name(), msg->getSignalName(), param_i-1, type->name);
                            type = msg->getType(param_i++);
                        }
                    }
                    base::debugLogData( BD_SIGNALDATA, msg->signal.getPayload(), msg->signal.getPayloadSize());

                    // Set capsule message for this inject.
                    msg->destPort->slot->capsule->msg = msg;

                    // Log the message (if enabled).
                    msg->destPort->slot->capsule->logMsg();

                    // Inject the signal into the capsule.
                    msg->destPort->slot->capsule->inject(*msg);
                }
            }
            // Put the message back in the pool (handles signal allocation also).
            umlrt::MessagePutToPool(msg);
        }
        // Do not wait if we have queued capsule messages left to inject.
        if (!_abort && !_exit && !capsuleQueue.count())
        {
            // Wait on the incoming queue or a timeout.
            wait();
        }
    }
    // Bug 468521 - must destroy owned capsules + slots here.

    // Remove this controller from the controller list.
    UMLRTCapsuleToControllerMap::removeController(name());

    // Leave this output in here for now.
    printf("Controller %s is %s exit value %p.\n", name(), _abort ? "aborting" : "exiting", exitValue);

    if (_abort)
    {
        // When aborting (versus exit) we output the messages.
        debugOutputMessages();
    }
    return(exitValue);
}

// Set the error code.
void UMLRTController::setError ( Error error )
{
    lastError = error;
    if ((error != E_OK) && (base::debugGetEnabledTypes() & (1 << BD_ERROR)) != 0)
    {
        BDEBUG(BD_ERROR, "Controller %s setError(%d):'%s'\n", name(), lastError, strerror());
    }
}

// Start the controller thread.
void UMLRTController::spawn ( )
{
    // No arguments for this thread.
    start(NULL);
}

// See umlrtcontroller.hh.
const char * UMLRTController::strerror ( ) const
{
    Error error = getError();
    if ((error < 0) || (error >= E_MAX))
    {
        SWERR("error code(%d) out of range max(%d)", error, E_MAX);
        return "unknown error code";
    }
    else
    {
        return errorToString[error];
    }
}

void UMLRTController::startTimer ( const UMLRTTimer * timer )
{
    char buf[UMLRTTimespec::TIMESPEC_TOSTRING_SZ];
    BDEBUG(BD_TIMER, "start timer(%p) destPort(%s) isInterval(%d) priority(%d) payloadSize(%d) due(%s)\n",
            timer, timer->sap()->getName(), timer->isInterval, timer->signal.getPriority(), timer->signal.getPayloadSize(),
            timer->isInterval ? timer->due.toStringRelative(buf, sizeof(buf)) : timer->due.toString(buf, sizeof(buf)));

    timerQueue.enqueue(timer);
}

// Wait on an external message or a timeout.
void UMLRTController::wait ( )
{
    // Linux-specific implementation for the time-being.

    // If there is a timer running, this holds the remaining time as the timeout of the select.
    struct timeval remainTimeval;

    // We default to 'wait forever', unless a timer is running.
    struct timeval * selectTimeval = NULL;

    bool wait = true; // Set this false if a timer is due or an incoming message appeared.

    // Get the time remaining on the first timer in the queue (if one exists).
    if (!timerQueue.isEmpty())
    {
        UMLRTTimespec remainTimespec = timerQueue.timeRemaining();

        if (remainTimespec.isZeroOrNegative())
        {
            // The timer is due - don't wait.
            wait = false;
            BDEBUG(BD_TIMER, "%s:timer is due\n", name());
        }
        else
        {
            // A timer is waiting but is not yet due. Set up the timeout. Will be non-zero.
            remainTimeval.tv_sec = remainTimespec.tv_sec;
            remainTimeval.tv_usec = remainTimespec.tv_nsec / 1000;

            char tmbuf[UMLRTTimespec::TIMESPEC_TOSTRING_SZ];
            BDEBUG(BD_TIMER, "%s: timer is not due - remain(%s)\n", name(), remainTimespec.toStringRelative(tmbuf, sizeof(tmbuf)));

            // Select will not wait forever.
            selectTimeval = &remainTimeval;
        }
    }
    if (!incomingQueue.isEmpty())
    {
        BDEBUG(BD_CONTROLLER, "%s: incoming q non-empty\n", name());
        wait = false;
    }
    if (wait)
    {
        // selectTimeval remains NULL if no timers are running. In that case, select will wait
        // forever until a message is delivered or a new timer is added to the timer-queue.

        // Get the queue notification file descriptors.
        int msgNotifyFd = incomingQueue.getNotifyFd();
        int timerFd = timerQueue.getNotifyFd();

        fd_set fds;

        FD_ZERO(&fds);
        FD_SET(msgNotifyFd, &fds);
        FD_SET(timerFd, &fds);

        // select wants to know the highest-numbered fd + 1.
        int nfds = msgNotifyFd + 1;
        if (timerFd > msgNotifyFd)
        {
            nfds = timerFd + 1;
        }

        // TODO - Bug 238 - DEBUG - remove this later.
        if (!selectTimeval)
        {
            // DEBUG - the intent is to wait forever until notification, since no timer is pending.
            // However, we wake the process up every 10 seconds during debugging.
            // No harm in doing that - the controller returns here if it finds nothing to do.
            remainTimeval.tv_sec = 10;
            remainTimeval.tv_usec = 0;
            selectTimeval = &remainTimeval;
        }
        // end DEBUG - remove this later

        BDEBUG(BD_CONTROLLER, "%s: call select msgfd(%d) timeout[%d,%d]\n",
                name(),
                msgNotifyFd,
                selectTimeval ? selectTimeval->tv_sec : -1,
                selectTimeval ? selectTimeval->tv_usec : -1);


        if ((select(nfds, &fds, NULL, NULL, selectTimeval)) < 0)
        {
            FATAL_ERRNO("select");
        }
        if (FD_ISSET(msgNotifyFd, &fds))
        {
            // Clear message notify-pending.
            incomingQueue.clearNotifyFd();
        }
        if (FD_ISSET(timerFd, &fds))
        {
            // Clear message notify-pending.
            timerQueue.clearNotifyFd();
        }
    }
}

/*static*/ void UMLRTController::debugOutputModelPortDeferQueueWalk ( const UMLRTMessage * msg, void *userData )
{
    int rtdata = *(int *)msg->signal.getPayload();
    size_t size = msg->signal.getPayloadSize();

    BDEBUG(BD_MODEL, "                            msg: id(%d) data [%d] %s\n",
            msg->signal.getId(),
            (size < sizeof(rtdata)) ? 0 : rtdata, (size < sizeof(rtdata)) ? "undef" : "");
}

void UMLRTController::debugOutputModelPortDeferQueue ( const UMLRTCommsPort * port )
{
    if (port != NULL)
    {
        if (port->deferQueue != NULL)
        {
            if (!port->deferQueue->isEmpty())
            {
                BDEBUG(BD_MODEL,
                        "                        defer queue:\n");
                port->deferQueue->walk( (UMLRTQueue::walk_callback_t)debugOutputModelPortDeferQueueWalk, NULL);
            }
        }
    }
}

void UMLRTController::debugOutputModelPort ( const UMLRTCommsPort * port, size_t index )
{
    if (!port)
    {
        BDEBUG(BD_MODEL,
"            port [%u]: (null)\n", index);
    }
    else
    {
        BDEBUG(BD_MODEL,
"            %s[%u] (id %d) %s%s %s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s\n",
            port->role()->name,
            port->role()->numFarEnd,
            port->role()->id,
            port->role()->protocol,
            port->role()->conjugated ? "~" : "",
            port->proxy ? "(proxy)" : "",
            port->spp ? "(SPP " : "",
            port->spp ? ((port->registeredName == NULL) ? "(unregistered)" : port->registeredName) : "",
            port->spp ? ")" : "",
            port->sap ? "(SAP " : "",
            port->sap ? ((port->registeredName == NULL) ? "(unregistered)" : port->registeredName) : "",
            port->sap ? ")" : "",
            port->automatic ? "(auto)" : "",
            port->notification ? "(notify)" : "",
            port->unbound ? "(unbound)" : "",
            port->relay ? "(relay)" : "",
            port->wired ? "(wired)" : "",
            port->locked ? "(locked)" : "",
            ((port->registrationOverride != NULL) && (port->registrationOverride[0] != '\0')) ? "(regovrd " : "",
            ((port->registrationOverride != NULL) && (port->registrationOverride[0] != '\0')) ? port->registrationOverride : "",
            ((port->registrationOverride != NULL) && (port->registrationOverride[0] != '\0')) ? ")" : ""
            );

        for (size_t j = 0; j < port->numFarEnd; ++j)
        {
            const char * farEndSlotName = "(none)";
            const char * farEndPortName = "(none)";
            const char * farEndSlotClass = "(no instance)";
            size_t farEndIndex = 0;
            if (port->farEnds[j].port != NULL)
            {
                farEndSlotName = port->farEnds[j].port->slotName();
                farEndPortName = port->farEnds[j].port->getName();
                farEndIndex = port->farEnds[j].farEndIndex;
                if (port->farEnds[j].port->slot->capsule != NULL)
                {
                    farEndSlotClass = port->farEnds[j].port->slot->capsule->getClass()->name;
                }
            }
            BDEBUG(BD_MODEL, "                    farEnd[%u] : -> { slot %s, port %s[%u] %s}\n",
                    j, farEndSlotName, farEndPortName, farEndIndex, farEndSlotClass );
        }
        debugOutputModelPortDeferQueue(port);
    }
}

void UMLRTController::debugOutputModelPortsArray ( size_t numPorts, const UMLRTCommsPort * ports )
{
    if (numPorts == 0)
    {
        BDEBUG(BD_MODEL,
"                ports        : (none)\n");
    }
    for (size_t i = 0; i < numPorts; ++i)
    {
        debugOutputModelPort(&ports[i], i);
    }
}

void UMLRTController::debugOutputModelPortsList ( size_t numPorts, const UMLRTCommsPort * * ports )
{
    if (numPorts == 0)
    {
        BDEBUG(BD_MODEL,
"                ports        : (none)\n");
    }
    for (size_t i = 0; i < numPorts; ++i)
    {
        debugOutputModelPort(ports[i], i);
    }
}

void UMLRTController::debugOutputClassInheritance ( const UMLRTCapsuleClass * capsuleClass )
{
    const UMLRTCapsuleClass * parent = capsuleClass->super;

    if (parent == NULL)
    {
        BDEBUG(BD_MODEL, "(none)");
    }
    while (parent != NULL)
    {
        BDEBUG(BD_MODEL, "-> %s ", parent->name);
        parent = parent->super;
    }
    BDEBUG(BD_MODEL,"\n");
}

void UMLRTController::debugOutputSlots ( const UMLRTSlot * slot )
{
    BDEBUG(BD_MODEL,
"    %s:\n", slot->name);
    BDEBUG(BD_MODEL,
"        capsule instance        : %-30s (%p)\n", slot->capsule ? slot->capsule->name() : "(none)", slot->capsule);
    if (slot->role() == NULL)
    {
    BDEBUG(BD_MODEL,
"        role                    : (none)\n");
    }
    else
    {
        BDEBUG(BD_MODEL,
"        role                    : %-30s [%d..%d] %s %s\n",
                slot->role()->name,
                slot->role()->multiplicityLower,
                slot->role()->multiplicityUpper,
                slot->role()->optional ? "optional " : "",
                slot->role()->plugin ? "plugin " : "");

    }
    BDEBUG(BD_MODEL,
"        index                   : %d\n", slot->capsuleIndex);
    BDEBUG(BD_MODEL,
"        class                   : %-30s (# sub-capsule roles : %u) (# border ports : %u) (# internal ports : %u)\n",
            slot->capsuleClass->name,
            slot->capsuleClass->numSubcapsuleRoles,
            slot->capsuleClass->numPortRolesBorder,
            slot->capsuleClass->numPortRolesInternal);
    BDEBUG(BD_MODEL,
"        class inheritance       : ");
    debugOutputClassInheritance(slot->capsuleClass);
    BDEBUG(BD_MODEL,
"        controller              : %s\n", slot->controller ? slot->controller->name() : "no owning controller");
    if (slot->slotToBorderMap == NULL)
    {
        BDEBUG(BD_MODEL,
"        slot to border map      : (none)\n");
    }
    else
    {
        BDEBUG(BD_MODEL,
"        slot to border map      :");
        for (size_t i = 0; i < slot->role()->capsuleClass->numPortRolesBorder; ++i)
        {
            BDEBUG(BD_MODEL, " [sl %lu=cp %d]", i, slot->slotToBorderMap[i]);
        }
        BDEBUG(BD_MODEL,"\n");
    }
    if (slot->numPorts == 0)
    {
        BDEBUG(BD_MODEL,
"        slot ports              : (none)\n");
    }
    else
    {
        BDEBUG(BD_MODEL,
"        slot ports              :\n");
        debugOutputModelPortsArray(slot->numPorts, slot->ports);
    }
    const UMLRTCommsPort * * internalPorts;
    const UMLRTCommsPort * * borderPorts;
    if (slot->capsule == NULL)
    {
        BDEBUG(BD_MODEL,
"        capsule border ports    : (none - no instance running in slot)\n");
    }
    else if ((borderPorts = slot->capsule->getBorderPorts()) == NULL)
    {
        BDEBUG(BD_MODEL,
"        capsule border ports    : (no border ports)\n");
    }
    else if (slot->capsuleClass->numPortRolesBorder == 0)
    {
        BDEBUG(BD_MODEL,
"        capsule border ports    : (class has no border ports)\n");
    }
    else
    {
        BDEBUG(BD_MODEL,
"        capsule border ports    : \n");
        debugOutputModelPortsList(slot->capsuleClass->numPortRolesBorder, borderPorts);
    }
    if (slot->capsule == NULL)
    {
        BDEBUG(BD_MODEL,
"        capsule internal ports  : (none)\n");
    }
    else if ((internalPorts = slot->capsule->getInternalPorts()) == NULL)
    {
        BDEBUG(BD_MODEL,
"        capsule internal ports  : (none)\n");
    }
    else
    {
        BDEBUG(BD_MODEL,
"        capsule internal ports  :\n");
        debugOutputModelPortsList(slot->capsuleClass->numPortRolesInternal, internalPorts);
    }
    // recurse into parts.
    if (slot->capsuleClass->numSubcapsuleRoles == 0)
    {
        BDEBUG(BD_MODEL,
"        # sub-capsule parts     : (none)\n");
    }
    else
    {
        BDEBUG(BD_MODEL,
"        # sub-capsule parts     : %d\n", slot->numParts);

        for (size_t i = 0; i < slot->numParts; ++i)
        {
            BDEBUG(BD_MODEL,
"            role [%u]: %s [%d..%d] %s %s\n",
                    i,
                    slot->parts[i].role()->name,
                    slot->parts[i].role()->multiplicityLower,
                    slot->parts[i].role()->multiplicityUpper,
                    (slot->parts[i].role()->optional) ? "optional " : "",
                            (slot->parts[i].role()->plugin) ? "plugin " : "");
        }
    }
    // Recurse into sub-structure outputing slot info.
    for (size_t i = 0; i < slot->numParts; ++i)
    {
        for (size_t j = 0; j < slot->parts[i].numSlot; ++j)
        {
            debugOutputSlots(slot->parts[i].slots[j]);
        }
    }
}

void UMLRTController::debugOutputSlotContainment ( const UMLRTSlot * slot, size_t nesting )
{
    for (size_t i = 0; i < nesting; ++i)
    {
        BDEBUG(BD_MODEL, "    ");
    }
    BDEBUG(BD_MODEL, "{ %s, %s, %p, %s }\n",
        slot->name,
        (slot->capsule == NULL) ? "(none)" : slot->capsule->name(),
         slot->capsule,
        slot->capsuleClass->name);
    for (size_t i = 0; i < slot->numParts; ++i)
    {
        for (size_t j = 0; j < slot->parts[i].numSlot; ++j)
        {
            debugOutputSlotContainment(slot->parts[i].slots[j], nesting + 1);
        }
    }
}

void UMLRTController::debugOutputModel ( const char * userMsg )
{
    // Acquire global RTS lock for this.
    UMLRTFrameService::rtsLock();

    char timebuf[UMLRTTimespec::TIMESPEC_TOSTRING_SZ];
    UMLRTTimespec tm;

    UMLRTTimespec::getclock(&tm);
    BDEBUG(BD_MODEL, "Model structure at time %s: %s\n", tm.toString(timebuf, sizeof(timebuf)), userMsg == NULL ? "" : userMsg);

    UMLRTCapsuleToControllerMap::debugOutputControllerList();

    UMLRTCapsuleToControllerMap::debugOutputCapsuleList();

    UMLRTProtocol::debugOutputServiceRegistration();

    const UMLRTCapsule * top = UMLRTCapsuleToControllerMap::getCapsuleFromName("Top");

    if (top == NULL)
    {
        BDEBUG(BD_MODEL, "ERROR: no 'Top' capsule found - no slot containment output.\n");
    }
    else
    {
        const UMLRTSlot * slot = top->getSlot();
        BDEBUG(BD_MODEL, "Slot containment: { <slot>, <capsule name>, <capsule instance address>, <capsule class> } \n");
        debugOutputSlotContainment(slot, 1);
    }
    if (top == NULL)
    {
        BDEBUG(BD_MODEL, "ERROR: no 'Top' capsule found - no slot list output.\n");
    }
    else
    {
        const UMLRTSlot * slot = top->getSlot();
        BDEBUG(BD_MODEL, "Slot list:\n");
        debugOutputSlots(slot);
    }
    UMLRTFrameService::rtsUnlock();
}

void UMLRTController::debugOutputMessage ( UMLRTMessage * msg )
{
    BDEBUG(BD_INJECT, "        capsule %s(role %s, class %s) port %s[%d]} signal id %d(%s) priority(%d)\n",
            msg->destSlot->capsule->name(), msg->destSlot->capsule->getName(),
            msg->destSlot->capsule->getTypeName(), msg->sap()->getName(), msg->sapIndex0(), msg->signal.getId(),
            msg->getSignalName(), msg->getPriority());
    msg->signal.debugOutputPayload();
}

void UMLRTController::debugOutputMessages ( )
{
    // Any queued messages are deallocated here.
    UMLRTFrameService::rtsLock();

    BDEBUG(BD_MODEL, "Messages queued for Controller %s:\n", name());
    // Queue messages associated with all timed-out timers.
    capsuleQueue.queueTimerMessages(&timerQueue);

    // Transfer all incoming messages to capsule queues.
    capsuleQueue.moveAll(incomingQueue);

    // Inject all available messages, highest priority msgs first.
    UMLRTMessage * msg;
    int index = 0;
    while ((msg = capsuleQueue.dequeueHighestPriority()) != NULL)
    {
        BDEBUG(BD_MODEL, "    message %d:\n", index++);
        debugOutputMessage(msg);
        umlrt::MessagePutToPool(msg);
    }
    if (!index)
    {
        BDEBUG(BD_MODEL, "    (none)\n");
    }
    UMLRTFrameService::rtsUnlock();
}
