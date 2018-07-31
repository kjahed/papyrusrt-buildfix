// umlrtobjectclass.cc

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#include "umlrtobjectclass.hh"
#include "basedebugtype.hh"
#include "basedebug.hh"
#include "basefatal.hh"
#include <stdlib.h>
#include <string>

// Type descriptor used for encoding/decoding data of a given type.
// A number of UMLRTType_xxx descriptors are pre-defined by the library.

uint8_t * globalDst = 0; // For debugging - initialized in umlrtsignalelement.cc
uint8_t * globalSrc = 0;

// 'dst' is being initialized - returns byte after the initialized field + plus any padding defined for it.
void * UMLRTObject_initialize ( const UMLRTObject_class * desc, void * dst )
{
    uint8_t * d = (uint8_t *)dst;

    if (desc->super != NULL)
    {
        d = (uint8_t *)desc->super->initialize(desc->super, d);
    }
    if (desc->object.numFields == 0)
    {
        memset(d, 0, desc->object.sizeOf);
        d += desc->object.sizeOf;
    }
    else
    {
        for (size_t i = 0; i < desc->object.numFields; ++i)
        {
            const UMLRTObject_class * fldDesc = desc->object.fields[i].desc;
            for (int j = 0; j < desc->object.fields[i].arraySize; ++j)
            {
                d = (uint8_t *)fldDesc->initialize(fldDesc, d);
            }
        }
    }
    return (void *)d;
}

// Default copy - returns pointer to byte after destination.
void * UMLRTObject_copy ( const UMLRTObject_class * desc, const void * src, void * dst )
{
    uint8_t * d = (uint8_t *)dst;

    if (desc->super != NULL)
    {
        d = (uint8_t *)desc->super->copy(desc->super, src, d); // Moves 'd' past base type data.
    }
    if (desc->object.numFields == 0)
    {
        BDEBUG(BD_SERIALIZE, "copy: desc(%s) size(%d) gs %ld gd %ld\n", desc->name, desc->object.sizeOf, (uint8_t *)src - globalSrc, (uint8_t *)d - globalDst);

        memcpy(d, src, desc->object.sizeOf);
        d += desc->object.sizeOf;
    }
    else
    {
        for (size_t i = 0; i < desc->object.numFields; ++i)
        {
            const UMLRTObject_field * fld = &desc->object.fields[i];
            uint8_t * s = (uint8_t *)src + fld->offset;
            d = (uint8_t *)dst + fld->offset;
            for (int j = 0; j < fld->arraySize; ++j)
            {
                BDEBUG(BD_SERIALIZE, "  copy: fld[%d]<%d> offset(%d) sizeDecoded(%d) (elem[%d] elemoff %ld gs %ld gd %ld)\n",
                        i, fld->name, fld->offset, fld->desc->object.sizeOf, j, s - (uint8_t *)src, s - globalSrc, (uint8_t *)d - globalDst);
                d = (uint8_t *)fld->desc->copy(fld->desc, s, d);
                s += fld->desc->object.sizeOf;
            }
        }
    }
    return d;
}

void * UMLRTObject_copy_charptr ( const UMLRTObject_class * desc, const void * src, void * dst )
{
    *((char * *) dst) = strdup(*((const char * *) src));
    if (*((char * *) dst) == NULL)
    {
        FATAL("Error duplicating string '%s'", (const char * *) src);
    }
    return (uint8_t *)dst + sizeof(char *);
}

// This returns the next byte after the source.
const void * UMLRTObject_decode ( const UMLRTObject_class * desc, const void * src, void * dst, int nest )
{
    uint8_t * s = (uint8_t *)src;

    if (desc->super != NULL)
    {
        s = (uint8_t *)desc->super->decode(desc->super, s, dst, nest);
    }
    if (desc->object.numFields == 0)
    {
        BDEBUG(BD_SERIALIZE, "decode: desc(%s) size(%d) gs %ld gd %ld\n", desc->name, desc->object.sizeOf, (uint8_t *)s - globalSrc, (uint8_t *)dst - globalDst);

        memcpy(dst, s, desc->object.sizeOf);
        s += desc->object.sizeOf;
    }
    else
    {
        for (size_t i = 0; i < desc->object.numFields; ++i)
        {
            const UMLRTObject_field * fld = &desc->object.fields[i];
            s = (uint8_t *)src + fld->offset;
            uint8_t * d = (uint8_t *)dst + fld->offset;
            for (int j = 0; j < fld->arraySize; ++j)
            {
                BDEBUG(BD_SERIALIZE, "  decode: fld[%d]<%d> offset(%d) sizeDecoded(%d) (elem[%d] elemoff %ld gs %ld gd %ld)\n",
                        i, fld->name, fld->offset, fld->desc->object.sizeOf, j, s - (uint8_t *)src, s - globalSrc, (uint8_t *)d - globalDst);
                s = (uint8_t *)fld->desc->decode(fld->desc, s, d, nest+1);
                d += fld->desc->object.sizeOf;
            }
        }
    }
    return s;
}

// This returns the next byte after the destination.
void * UMLRTObject_encode ( const UMLRTObject_class * desc, const void * src, void * dst, int nest )
{
    uint8_t * d = (uint8_t *)src;

    if (desc->super != NULL)
    {
        d = (uint8_t *)desc->super->encode(desc->super, src, d, nest);
    }
    if (desc->object.numFields == 0)
    {
        BDEBUG(BD_SERIALIZE, "copy: desc(%s) size(%d) gs %ld gd %ld\n", desc->name, desc->object.sizeOf, (uint8_t *)src - globalSrc, (uint8_t *)d - globalDst);

        memcpy(d, src, desc->object.sizeOf);
        d += desc->object.sizeOf;
    }
    else
    {
        for (size_t i = 0; i < desc->object.numFields; ++i)
        {
            const UMLRTObject_field * fld = &desc->object.fields[i];
            uint8_t * s = (uint8_t *)src + fld->offset;
            d = (uint8_t *)dst + fld->offset;
            for (int j = 0; j < fld->arraySize; ++j)
            {
                BDEBUG(BD_SERIALIZE, "  encode: fld[%d]<%d> offset(%d) sizeDecoded(%d) (elem[%d] elemoff %ld gs %ld gd %ld)\n",
                        i, fld->name, fld->offset, fld->desc->object.sizeOf, j, s - (uint8_t *)src, s - globalSrc, (uint8_t *)d - globalDst);
                d = (uint8_t *)fld->desc->encode(fld->desc, s, d, nest+1);
                s += fld->desc->object.sizeOf;
            }
        }
    }
    return d;
}

// Default destroy does nothing (but returns a pointer to the byte after the data).
void * UMLRTObject_destroy ( const UMLRTObject_class * desc, void * data )
{
    return (void *)((uint8_t *)data + desc->object.sizeOf);
}

void * UMLRTObject_destroy_charptr ( const UMLRTObject_class * desc, void * data )
{
    free(*((char * *) data));
    return (uint8_t *)data + sizeof(char *);
}

// Assumes 'decoded' data (not 'encoded' data).
int UMLRTObject_fprintf ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;

    // Debug
    if (nest == 0)
    {
        globalSrc = (uint8_t *)data;
    }
    for (int ai = 0; ai < arraySize; ++ai)
    {
        uint8_t * ai_base = (uint8_t *)data + (desc->object.sizeOf * ai);
        if (desc->object.numFields != 0)
        {
            BDEBUG(BD_SERIALIZE, "\n");
        }
        for (int n = 0; n < nest; ++n)
        {
            BDEBUG(BD_SERIALIZE, "    ");
        }
        nchar += fprintf(ostream, "{%s", desc->name);
        if (arraySize > 1)
        {
            nchar += fprintf(ostream, "[%d]", ai);
        }
        nchar += fprintf(ostream, ":");
        BDEBUG(BD_SERIALIZE, "\n");
        if (desc->object.numFields != 0)
        {
            for (size_t fi = 0; fi < desc->object.numFields; ++fi)
            {
                const UMLRTObject_field * fld = &desc->object.fields[fi];
                uint8_t * fi_base = ai_base + fld->offset;
                //nchar += fprintf(ostream, "\n");
                for (int n = 0; n < nest; ++n)
                {
                    BDEBUG(BD_SERIALIZE, "    ");
                }
                nchar += fprintf(ostream, "{%s:", fld->name);
                BDEBUG(BD_SERIALIZE, "<desc(%s) arraySize %d elem %d field %d gs %ld>",
                        desc->name, arraySize, ai, fi, fi_base - globalSrc);
                for (int n = 0; n < nest; ++n)
                {
                    BDEBUG(BD_SERIALIZE, "    ");
                }
                nchar += fld->desc->fprintf(ostream, fld->desc, fi_base, nest+1, fld->arraySize);
                BDEBUG(BD_SERIALIZE, "  ");
                for (int n = 0; n < nest; ++n)
                {
                    BDEBUG(BD_SERIALIZE, "    ");
                }
                nchar += fprintf(ostream, "}");
                BDEBUG(BD_SERIALIZE, "\n");
            }
        }
        else if ((desc->fprintf != NULL) && (desc->fprintf != UMLRTObject_fprintf))
        {
            // Don't allow a recursive call with the same parameters.
            nchar += desc->fprintf(ostream, desc, ai_base, nest+1, arraySize);
        }
        else
        {
            nchar += fprintf(ostream, "(unable to print)"); // Either no fprintf defined or malformed descriptor.
            BDEBUG(BD_SERIALIZE, "\n");
        }
        for (int n = 0; n < nest; ++n)
        {
            BDEBUG(BD_SERIALIZE, "    ");
        }
        nchar += fprintf(ostream, "}");
        BDEBUG(BD_SERIALIZE, "\n");
    }
    return nchar;
}

static int UMLRTObject_fprintf_bool ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    bool b;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(b)), &b);
        nchar += fprintf(ostream, "{bool %s}", b ? "true" : "false");
    }
    return nchar;
}

static int UMLRTObject_fprintf_char ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    nchar += fprintf(ostream, "{char ");
    for (int i = 0; i < arraySize; ++i)
    {
        nchar += fprintf(ostream, "0x%02X ", *((char*)data + i));
    }
    nchar += fprintf(ostream, " '");
    for (int i = 0; i < arraySize; ++i)
    {
        char c = *((char*)data + i);
        nchar += fprintf(ostream, "%c", (c > 0x1F) && (c < 0x7F) ? c : '.');
    }
    nchar += fprintf(ostream, "'}");
    return nchar;
}

static int UMLRTObject_fprintf_double ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    double d;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(d)), &d);
        nchar += fprintf(ostream, "{double %f}", d);
    }
    return nchar;
}

static int UMLRTObject_fprintf_float ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    float f;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(f)), &f);
        nchar += fprintf(ostream, "{float %f}", f);
    }
    return nchar;
}

static int UMLRTObject_fprintf_int ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    int iv;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(iv)), &iv);
        nchar += fprintf(ostream, "{int %d}", iv);
    }
    return nchar;
}

static int UMLRTObject_fprintf_long ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    long l;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(l)), &l);
        nchar += fprintf(ostream, "{long %ld}", l);
    }
    return nchar;
}

static int UMLRTObject_fprintf_longdouble ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    long double ld;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(ld)), &ld);
        nchar += fprintf(ostream, "{longdouble %Lf}", ld);
    }
    return nchar;
}

static int UMLRTObject_fprintf_longlong ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    long long ll;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(ll)), &ll);
        nchar += fprintf(ostream, "{longlong %lld}", ll);
    }
    return nchar;
}

static int UMLRTObject_fprintf_ptr ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    void * p;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(p)), &p);
        // Pointer de-referencing tracked by Bug 468512.
        nchar += fprintf(ostream, "{ptr %p}", p);
    }
    return nchar;
}

static int UMLRTObject_fprintf_charptr ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    void * p;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(p)), &p);
        // Pointer de-referencing tracked by Bug 468512.
        nchar += fprintf(ostream, "{charptr %p} \"%s\"", p, (char *) p);
        desc->destroy(desc, &p);
    }
    return nchar;
}

static int UMLRTObject_fprintf_short ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    short sh;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(sh)), &sh);
        nchar += fprintf(ostream, "{short %d}", sh);
    }
    return nchar;
}

static int UMLRTObject_fprintf_uchar ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    unsigned char uc;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i), &uc);
        nchar += fprintf(ostream, "{uchar %u}", uc);
    }
    return nchar;
}

static int UMLRTObject_fprintf_uint ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    unsigned int ui;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(ui)), &ui);
        nchar += fprintf(ostream, "{uint %u}", ui);
    }
    return nchar;
}

static int UMLRTObject_fprintf_ulong ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    unsigned long ul;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(ul)), &ul);
        nchar += fprintf(ostream, "{ulong %lu}", ul);
    }
    return nchar;
}

static int UMLRTObject_fprintf_ulonglong ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    unsigned long long ll;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(ll)), &ll);
        nchar += fprintf(ostream, "{ulonglong %llu}", ll);
    }
    return nchar;
}

static int UMLRTObject_fprintf_ushort ( FILE *ostream, const UMLRTObject_class * desc, const void * data, int nest, int arraySize )
{
    int nchar = 0;
    unsigned short ush;
    for (int i = 0; i < arraySize; ++i)
    {
        desc->copy(desc, ((uint8_t*)data + i*sizeof(ush)), &ush);
        nchar += fprintf(ostream, "{ushort %u}", ush);
    }
    return nchar;
}

const UMLRTObject_class UMLRTType_bool
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_bool,
        "bool",
        NULL, // super
        {sizeof(bool), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_char
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_char,
        "char",
        NULL, // super
        {sizeof(char), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_double
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_double,
        "double",
        NULL, // super
        {sizeof(double), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_float
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_float,
        "float",
        NULL, // super
        {sizeof(float), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_int
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_int,
        "int",
        NULL, // super
        {sizeof(int), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_long
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_long,
        "long",
        NULL, // super
        {sizeof(long), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_longdouble
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_longdouble,
        "longdouble",
        NULL, // super
        {sizeof(long double), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_longlong
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_longlong,
        "longlong",
        NULL, // super
        {sizeof(long long), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_ptr
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_ptr,
        "ptr",
        NULL, // super
        {sizeof(void *), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_charptr
= {
        UMLRTObject_initialize,
        UMLRTObject_copy_charptr,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy_charptr,
        UMLRTObject_fprintf_charptr,
        "charptr",
        NULL, // super
        {sizeof(char *), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_short
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_short,
        "short",
        NULL, // super
        {sizeof(short), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_uchar
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_uchar,
        "uchar",
        NULL, // super
        {sizeof(unsigned char), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_uint
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_uint,
        "uint",
        NULL, // super
        {sizeof(unsigned int), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_ulong
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_ulong,
        "ulong",
        NULL, // super
        {sizeof(unsigned long), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_ulonglong
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_ulonglong,
        "ulonglong",
        NULL, // super
        {sizeof(unsigned long long), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTType_ushort
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf_ushort,
        "ushort",
        NULL, // super
        {sizeof(unsigned short), 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

const UMLRTObject_class UMLRTObject_empty
= {
        UMLRTObject_initialize,
        UMLRTObject_copy,
        UMLRTObject_decode,
        UMLRTObject_encode,
        UMLRTObject_destroy,
        UMLRTObject_fprintf,
        "empty",
        NULL, // super
        {0, 0, NULL}, // object
        UMLRTOBJECTCLASS_DEFAULT_VERSION, // version
        UMLRTOBJECTCLASS_DEFAULT_BACKWARDS, // backwards
};

UMLRTTypedValue new_UMLRTTypedValue ( bool const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_bool);
}

UMLRTTypedValue new_UMLRTTypedValue ( char const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_char);
}

UMLRTTypedValue new_UMLRTTypedValue ( double const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_double);
}

UMLRTTypedValue new_UMLRTTypedValue ( int const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_int);
}

UMLRTTypedValue new_UMLRTTypedValue ( long const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_long);
}

UMLRTTypedValue new_UMLRTTypedValue ( long long const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_longlong);
}

UMLRTTypedValue new_UMLRTTypedValue ( long double const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_longdouble);
}

UMLRTTypedValue new_UMLRTTypedValue ( short const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_short);
}

UMLRTTypedValue new_UMLRTTypedValue ( const void * const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_ptr);
}

UMLRTTypedValue new_UMLRTTypedValue ( const char * const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_charptr);
}

UMLRTTypedValue new_UMLRTTypedValue ( unsigned char const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_uchar);
}

UMLRTTypedValue new_UMLRTTypedValue ( unsigned int const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_uint);
}

UMLRTTypedValue new_UMLRTTypedValue ( unsigned long const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_ulong);
}

UMLRTTypedValue new_UMLRTTypedValue ( unsigned long long const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_ulonglong);
}

UMLRTTypedValue new_UMLRTTypedValue ( unsigned short const & value )
{
    return UMLRTTypedValue(&value, &UMLRTType_ushort);
}
