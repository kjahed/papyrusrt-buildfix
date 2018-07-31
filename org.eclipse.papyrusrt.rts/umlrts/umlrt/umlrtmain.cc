// umlrtmain.cc

/*******************************************************************************
* Copyright (c) 2014-2017 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "umlrtmain.hh"
#include "basefatal.hh"
#include "basedebug.hh"
#include "umlrtgetopt.hh"
#include "umlrtcapsuletocontrollermap.hh"
#include "umlrtcontroller.hh"

// See umlrtmain.hh for documentation.

// This set of argc, argv only contain user-arguments and do not contain arguments
// intended for the RTS-library.
int UMLRTMain::argc = 0;
const char * * UMLRTMain::argv = NULL;
bool UMLRTMain::argsDefined = false;

// Help text for usage output
static UMLRTMain::t_usage_option_help optionhelp[] = {

        { "General RTS options", 0, "", "" },
        { "help",           'h', "",            "Print usage and exit." },
        { "userargs",       'u', "",            "Marks the start of application options." },
        { "logmsg",         'l', "",            "Enable capsule logMsg output of injected signals." },
        { "controllers",    'c', "<controllers-file>", "Specify a capsule-to-controller map file." },
        { "Debug feature enable options", 0, "", "" },
        { "debug",          'D', "0/1",         "Overall debug log enable." },
        { "debugcolor",     'C', "0/1",         "Disable/enable terminal text color escape sequences." },
        { "debugsummary",   's', "",            "Output debug summary." },
        { "debugtypeon",    'T', "<type>",      "Enable debug types. See <type> below." },
        { "debugtypeoff",   't', "<type>",      "Disable debug types. See <type> below." },
        { "debugmodel",     'M', "",            "Output instance UML-RT model after startup. (Usually also requires '-T model')" },
        { "Enabling invidual log message components", 0, "", "" },
        { "debugtime",      'S', "0/1",         "Disable/enable log time-stamp." },
        { "debugname",      'N', "0/1",         "Disable/enable log type name." },
        { "debugfile",      'F', "0/1",         "Disable/enable log file-name." },
        { "debugline",      'L', "0/1",         "Disable/enable log file line #." },
        { "debugthread",    'n', "0/1",         "Disable/enable log thread name (or id)." },
        { "debugmethod",    'r', "0/1",         "Disable/enable log method name." },
        { "debugmsg",       'm', "0/1",         "Disable/enable log application message." },
        { NULL, 0, NULL, NULL } // MUST BE LAST to terminate loop
};

/*static*/ bool UMLRTMain::optionsValid( t_usage_option_help options[], int argc, char * const * argv )
{
    // Because the following BDEBUG macros are invoked before command-line options are processed, no output will ever
    // be generated unless debugEnableMessageType (basedebug.cc) is hard-coded to have BD_MAIN bit set and
    // debugEnable is hard-coded to 'true'.
    if (base::debugTypeEnabled(BD_MAIN))
    {
        BDEBUG(BD_MAIN, "validate argc_(%d) ", argc);
        for (int i = 0; i < argc; ++i)
        {
            BDEBUG(BD_MAIN, "argv_[%d] '%s' ", i, argv[i]);
        }
        BDEBUG(BD_MAIN, "\n");
    }
    // Bug 471496
    // This code checks to see if an option that requires an argument appears as the last option and the user
    // did not specify an argument. getopt_long in g++ (Ubuntu 4.8.2-19ubuntu1) 4.8.2 fails to detect the error and returns
    // garbage in 'optarg'. This code will detect the scenario so that getopt_long is not relied upon to detect it.
    bool valid = true;
    if (argc > 0)
    {
        // At least one argument supplied.
        char * lastarg = argv[argc-1];
        if ((strlen(lastarg) > 1) && (lastarg[0] == '-'))
        {
            // Last argument is an option (either short or long.)
            bool islong = ((strlen(lastarg) > 2) && (lastarg[1] == '-'));
            lastarg += 1; // Skip leading '-' for short arguments.
            if (islong)
            {
                // Skip second leading '-' for long arguments.
                lastarg += 1;
            }
            BDEBUG(BD_MAIN, "verify lastarg(%s) islong(%d)\n", lastarg, islong);
            t_usage_option_help * opt = NULL;
            for (int i = 0; (options[i].longopt != NULL) && (opt == NULL); ++i)
            {
                if (islong && !strncmp(options[i].longopt, lastarg, strlen(options[i].longopt)))
                {
                    if (strlen(lastarg) == (strlen(options[i].longopt)))
                    {
                        // long option was specified last with no '=' appended to it and no argument.
                        opt = &options[i];
                    }
                    else if ((strlen(lastarg) == (strlen(options[i].longopt) + 1)) && (lastarg[strlen(lastarg)-1] == '='))
                    {
                        // long option was specified last with '=' appended to it and no argument.
                        opt = &options[i];
                    }
                }
                else if (!islong && (strlen(lastarg) == 1) && (lastarg[0] == options[i].shortopt))
                {
                    opt = &options[i];
                }
            }
            if (opt != NULL)
            {
                BDEBUG(BD_MAIN, "opt found '%s' '%c'\n", opt->longopt, opt->shortopt);
                if (strlen(opt->value))
                {
                    printf("ERROR: option '%s' ('%c') (%s) requires an argument (%s)\n",
                            opt->longopt, opt->shortopt, opt->help, opt->value);
                    valid = false;
                }
            }
        }
    }
    return valid;
}

// Output usage information and exit
/*static*/ void UMLRTMain::usage( const char * const program )
{
    // Compute field widths for column-aligned output
    int maxlongoptlen = 0;
    int maxvaluelen = 0;
    for (int i = 0; optionhelp[i].longopt; ++i)
    {
        int len;
        if (optionhelp[i].shortopt == 0)
        {
            // Is a title for a group of options. Length doesn't apply.
        }
        else
        {
            if ((len = strlen(optionhelp[i].longopt)) > maxlongoptlen)
            {
                maxlongoptlen = len;
            }
            if ((len = strlen(optionhelp[i].value)) > maxvaluelen)
            {
                maxvaluelen = len;
            }
        }
    }
    // Terminal colour - must be reset at end.
    base::debugColourBrightRed();

    // Summary usage line
    printf("usage:\n");
    printf("  %s ", program);
    for (int i = 0; optionhelp[i].longopt; ++i)
    {
        if (optionhelp[i].shortopt == 0)
        {
            // Is not an option, but contains a title for a group of options.
        }
        else if (optionhelp[i].value[0] == '\0')
        {
            printf("[-%c] ", optionhelp[i].shortopt);
        }
        else
        {
            printf("[-%c%s] ", optionhelp[i].shortopt, optionhelp[i].value);
        }
    }
    printf("\n");

    // Option summary
    for (int i = 0; optionhelp[i].longopt; ++i)
    {
        int sofar;
        if (optionhelp[i].shortopt == 0)
        {
            printf("\n%s:\n", optionhelp[i].longopt);
        }
        else
        {
            if (optionhelp[i].value[0] == '\0')
            {
                sofar = printf("[-%c]", optionhelp[i].shortopt);
            }
            else
            {
                sofar = printf("[-%c %s]", optionhelp[i].shortopt, optionhelp[i].value);
            }
            for (int j = sofar; j < (maxvaluelen + 6); ++j)
            {
                printf(" ");
            }
            if (optionhelp[i].value[0] == '\0')
            {
                sofar = printf(" or [--%s]", optionhelp[i].longopt);
            }
            else
            {
                sofar = printf(" or [--%s=%s]", optionhelp[i].longopt, optionhelp[i].value);
            }
            for (int j = sofar; j < (maxlongoptlen + maxvaluelen + 10); ++j)
            {
                printf(" ");
            }
            printf("%s\n", optionhelp[i].help);
        }
    }
    printf("\n<type> is a string that matches (case-insensitive) one of the following debug-types:\n"
           "Current types are ");
    base::debugTypeSummary();
    printf("\n");
    base::debugColourReset();

    exit(EXIT_FAILURE);
}

// Extract user arguments and define argument list and count for user.
// Currently doesn't do anything with arguments specified before '--userargs'.
/*static*/ void UMLRTMain::setArgs( int argc_, char * const * argv_ )
{
   static struct option options[] = {
            { "help",               no_argument,       NULL, 'h' },
            { "controllers",        required_argument, NULL, 'c' },
            { "debug",              required_argument, NULL, 'D' },
            { "debugcolor",         required_argument, NULL, 'C' },
            { "debugtypeon",        required_argument, NULL, 'T' },
            { "debugtypeoff",       required_argument, NULL, 't' },
            { "debugname",          required_argument, NULL, 'N' },
            { "debugtime",          required_argument, NULL, 'S' },
            { "debugthread",        required_argument, NULL, 'n' },
            { "debugfile",          required_argument, NULL, 'F' },
            { "debugline",          required_argument, NULL, 'L' },
            { "debugmethod",        required_argument, NULL, 'f' },
            { "debugmsg",           required_argument, NULL, 'm' },
            { "debugsummary",       no_argument,       NULL, 's' },
            { "debugmodel",         no_argument,       NULL, 'M' },
            { "userargs",           no_argument,       NULL, 'u' },
            { "logmsg",             no_argument,       NULL, 'l' },
            { NULL,                 0,                 NULL, 0 },
    };

    int userargstart = argc_; // No user arguments unless '--userargs' is found.
    bool userargsfound = false;
    int opti = 1; // getopt_long skips program name.
    int optchar;

    bool debugsummary = false;
    bool displayusage = false;
    bool displaymodel = false;
    char * controllerfile = NULL;
    int longindex = 0;
    bool optionsvalid = optionsValid(optionhelp, argc_, argv_);

    while (optionsvalid && ((optchar = getopt_long( argc_, argv_, ":hc:D:C:T:t:N:S:n:F:L:f:m:Msul", options, &longindex)) != -1) && !userargsfound)
    {
        BDEBUG(BD_MAIN, "optchar as %%c(%c) optind(%d) longindex(%d) optarg(%s)\n", optchar, optind, longindex, optarg != NULL ? optarg : "NULL");
        switch(optchar)
        {
        case 'h':
            displayusage = true;
            break;
        case 'c':
            controllerfile = optarg;
            break;
        case 'D':
            base::debugEnableSet(atoi(optarg) != 0);
            break;
        case 'C':
            base::debugEnableColorSet(atoi(optarg) != 0);
            break;
        case 'T':
            if (!base::debugEnableTypeMaskSpecSet(optarg, true /*will enable*/))
            {
                usage(argv_[0]);
            }
            break;
        case 't':
            if (!base::debugEnableTypeMaskSpecSet(optarg, false /*will disable*/))
            {
                usage(argv_[0]);
            }
            break;
        case 'N':
            base::debugEnableTypeNameDisplaySet(atoi(optarg) != 0);
            break;
        case 'S':
            base::debugEnableTimeDisplaySet(atoi(optarg) != 0);
            break;
        case 'n':
            base::debugEnableThreadDisplaySet(atoi(optarg) != 0);
            break;
        case 'F':
            base::debugEnableFilenameDisplaySet(atoi(optarg) != 0);
            break;
        case 'L':
            base::debugEnableLineNumberDisplaySet(atoi(optarg) != 0);
            break;
        case 'f':
            base::debugEnableMethodDisplaySet(atoi(optarg) != 0);
            break;
        case 'm':
            base::debugEnableUserMsgDisplaySet(atoi(optarg) != 0);
            break;
        case 'M':
            displaymodel = true;
            break;
        case 's':
            debugsummary = true;
            break;
        case 'u':
            userargsfound = true;
            userargstart = opti + 1;
            break;
        case 'l':
            base::debugEnableTypeSet(BD_LOGMSG, true /*will enable*/);
            break;
        default:
            usage(argv_[0]);
        }
        opti++;
    }
    BDEBUG(BD_MAIN,"optind(%d) userargsfound(%s) valid(%s) optchar as %%d(%d)\n", optind, userargsfound ? "true" : "false", optionsvalid ? "true" : "false", optchar);
    if (!optionsvalid)
    {
        // User error message has already been output.
        usage(argv_[0]);
    }
    if (!userargsfound && (optind < argc_))
    {
        printf("ERROR: Command-line argument '%s' not recognized.\n", argv_[optind]);
        usage(argv_[0]);
    }
    if (controllerfile)
    {
        // If user is reassigning the capsule controllers with a file...
        if (!UMLRTCapsuleToControllerMap::readCapsuleControllerMap(controllerfile))
        {
            printf("ERROR: failed to create capsule-to-controller map. Exiting.\n");
            usage(argv_[0]);
        }
        UMLRTCapsuleToControllerMap::debugOutputCaspuleToControllerMap();
    }

    // If user requested debug options summary...
    if (debugsummary)
    {
        base::debugOptionSummary();
    }
    // If the user requested help, print usage (exit)
    if (displayusage)
    {
        usage(argv_[0]);
    }
    if (displaymodel)
    {
        if ((base::debugGetEnabledTypes() & (1 << BD_MODEL)) == 0)
        {
            printf("WARNING: specified -M (--debugmodel) and '-T model' is not enabled.\n");
        }
        else
        {
            UMLRTController * controller = (UMLRTController *)UMLRTCapsuleToControllerMap::getFirstController();
            if (controller == NULL)
            {
                printf("WARNING: no controllers instantiated?\n");
            }
            else
            {
                // Controllers are not actually running yet - use the synchronous output to capture the model before any run-time activity.
                controller->debugOutputModel("initial instance model");
            }
        }
    }
    argc = argc_ - userargstart;
    if (userargstart < argc_)
    {
        argv = new const char *[argc];
    }
    for (int i = 0, j = userargstart; userargsfound && (j < argc_); ++j)
    {
        argv[i++] = argv_[j];
    }
    argsDefined = true; // Sanity checking on user fetching of arguments.
}

// Get the number of user command-line arguments.
/*static*/ int UMLRTMain::getArgCount()
{
    if (!argsDefined)
    {
        FATAL("getArgCount() called before setArgs()");
    }
    return argc;
}

// Get the i-th command-line argument.
/*static*/ const char * UMLRTMain::getArg( int index )
{
    if (!argsDefined)
    {
        FATAL("getArg(%d) called before setArgs()", index);
    }
    if ((index < 0) || (index > (argc-1)))
    {
        FATAL("getArg(%d) called but max index is %d", index, (argc-1));
    }
    return argv[index];
}
