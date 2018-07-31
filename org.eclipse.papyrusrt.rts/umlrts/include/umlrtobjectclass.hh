// umlrtobjectclass.hh

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#ifndef UMLRTOBJECTCLASS_HH
#define UMLRTOBJECTCLASS_HH

#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

// Type descriptors used for encoding/decoding data and passing user data within the RTS API.
// A number of UMLRTType_xxx descriptors are pre-defined by the library. See below.

// Every type descriptor is assigned two values ('version' and 'backwards') to capture
// version compatibility to allow for the evolution of the type description over time
// (i.e. different versions of the RTS library and associated modeling tool) and provide
// compatibility information for decoding the data across multiple versions of the code, etc.

// Initially 'version' and 'backwards' are both set to 0.

// A new 'version' can be created by appending new data to the end of a type (i.e.
// incrementing the 'version') while not changing existing data definitions (i.e. preserving
// the backwards-compatibility of already-defined fields i.e. NOT incrementing the 'backwards').
// Thus, some older code will be able to decode new versions (because these are backwards compatible.

#define UMLRTOBJECTCLASS_DEFAULT_VERSION   1
#define UMLRTOBJECTCLASS_DEFAULT_BACKWARDS 1

struct UMLRTObject_class;

// A composite type can be made up of a set of 'fields'. Each field has a 'type' and
// and an offset. The 'ptrIndirectionCount' defines pointers and specifies how to get
// at the associated data for serialization.
struct UMLRTObject_field
{
    const char * name;
    const UMLRTObject_class * const desc;
    int offset;
    int arraySize;
    int ptrIndirection;
};

// Basic object description without API.

struct UMLRTObject
{
    const size_t sizeOf;
    const size_t numFields;
    const UMLRTObject_field * const fields;
};

// All types are described by a 'data descriptor', namely a UMLRTObject_class.
// Each descriptor is given a 'name'.
// Types can be derived from other types ('super' specifying the base type.)
// Simple data types (representing a block of memory) have a 'size' and no 'fields'.
// A composite type has an array of 'fields' describing the data components of the type.
// Each descriptor contains a set of function-pointers for initializing, copying,
// encoding, decoding and destroying.

// A set of pre-defined types are defined for basic data types (see below).
// The code generator will generate the UMLRTObject_class descriptors for user-defined
// types in the model.

struct UMLRTObject_class
{
    void * ( * initialize ) ( const UMLRTObject_class * desc, void * data );
    void * ( * copy ) ( const UMLRTObject_class * desc, const void * src, void * dst );
    // decode returns pointer to next byte in decoded bytes.
    const void * ( * decode ) ( const UMLRTObject_class * desc, const void * src, void * dst, int nest );
    // encode returns pointer to next byte in encoded bytes.
    void * ( * encode ) ( const UMLRTObject_class * desc, const void * src, void * dst, int nest );
    void * ( * destroy ) ( const UMLRTObject_class * desc, void * data );
    int ( * fprintf ) ( FILE * ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize ); // returns number of chars output.

    const char * name;
    const UMLRTObject_class * const super; // Base type
    const UMLRTObject object;
    const uint16_t version;
    const uint16_t backwards;
};

// UMLRTTypedValue encapsulates both a pointer to data and its type descriptor.
struct UMLRTTypedValue
{
    UMLRTTypedValue ( const void * data_ = ( void * )0, const UMLRTObject_class * desc = ( const UMLRTObject_class *)0 ) : type(desc), data(data_) { }
    UMLRTTypedValue ( const UMLRTTypedValue & other ) : type(other.type), data(other.data) { }
    const UMLRTObject_class * type;
    const void * data;
};

// A set of default functions for basic data-descriptions (types) are pre-defined.
extern void * UMLRTObject_initialize ( const UMLRTObject_class * desc, void * data );
extern void * UMLRTObject_copy ( const UMLRTObject_class * desc, const void * src, void * dst );
extern const void * UMLRTObject_decode ( const UMLRTObject_class * desc, const void * src, void * dst, int nest );
extern void * UMLRTObject_encode ( const UMLRTObject_class * desc, const void * src, void * dst, int nest );
extern void * UMLRTObject_destroy ( const UMLRTObject_class * desc, void * data );
extern size_t UMLRTObject_getSize ( const UMLRTObject_class * desc );

// The following returns the number of characters that were printed.
extern int UMLRTObject_fprintf ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest = 0, int arraySize = 1 );

// These are the primitive data-types with pre-defined data descriptors.
extern const UMLRTObject_class UMLRTType_bool;
extern const UMLRTObject_class UMLRTType_char;
extern const UMLRTObject_class UMLRTType_double;
extern const UMLRTObject_class UMLRTType_float;
extern const UMLRTObject_class UMLRTType_int;
extern const UMLRTObject_class UMLRTType_long;
extern const UMLRTObject_class UMLRTType_longdouble;
extern const UMLRTObject_class UMLRTType_longlong;
extern const UMLRTObject_class UMLRTType_ptr;
extern const UMLRTObject_class UMLRTType_charptr;
extern const UMLRTObject_class UMLRTType_short;
extern const UMLRTObject_class UMLRTType_uchar;
extern const UMLRTObject_class UMLRTType_uint;
extern const UMLRTObject_class UMLRTType_ulong;
extern const UMLRTObject_class UMLRTType_ulonglong;
extern const UMLRTObject_class UMLRTType_ushort;

extern const UMLRTObject_class UMLRTObject_empty;

// Overloaded functions yielding basic descriptors:
extern UMLRTTypedValue new_UMLRTTypedValue ( bool         const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( char         const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( double       const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( float        const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( int          const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( long         const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( long long    const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( long double  const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( short        const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( const void * const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( const char * const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( unsigned char      const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( unsigned int       const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( unsigned long      const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( unsigned long long const & );
extern UMLRTTypedValue new_UMLRTTypedValue ( unsigned short     const & );

#endif // UMLRTOBJECTCLASS_HH
