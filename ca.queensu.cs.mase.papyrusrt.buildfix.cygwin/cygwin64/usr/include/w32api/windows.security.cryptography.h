/*** Autogenerated by WIDL 1.5.31 from include/windows.security.cryptography.idl - Do not edit ***/

#ifndef __REQUIRED_RPCNDR_H_VERSION__
#define __REQUIRED_RPCNDR_H_VERSION__ 475
#endif

#include <rpc.h>
#include <rpcndr.h>

#ifndef COM_NO_WINDOWS_H
#include <windows.h>
#include <ole2.h>
#endif

#ifndef __windows_security_cryptography_h__
#define __windows_security_cryptography_h__

/* Forward declarations */

#ifndef __ICryptographicBufferStatics_FWD_DEFINED__
#define __ICryptographicBufferStatics_FWD_DEFINED__
typedef interface ICryptographicBufferStatics ICryptographicBufferStatics;
#endif

/* Headers for imported files */

#include <inspectable.h>
#include <windows.storage.streams.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifndef __IBuffer_FWD_DEFINED__
#define __IBuffer_FWD_DEFINED__
typedef interface IBuffer IBuffer;
#endif

#ifndef __ICryptographicBufferStatics_FWD_DEFINED__
#define __ICryptographicBufferStatics_FWD_DEFINED__
typedef interface ICryptographicBufferStatics ICryptographicBufferStatics;
#endif

enum BinaryStringEncoding;

typedef enum BinaryStringEncoding {
    Utf8 = 0,
    Utf16LE = 1,
    Utf16BE = 2
} BinaryStringEncoding;
/*****************************************************************************
 * ICryptographicBufferStatics interface
 */
#ifndef __ICryptographicBufferStatics_INTERFACE_DEFINED__
#define __ICryptographicBufferStatics_INTERFACE_DEFINED__

DEFINE_GUID(IID_ICryptographicBufferStatics, 0x320b7e22, 0x3cb0, 0x4cdf, 0x86,0x63, 0x1d,0x28,0x91,0x00,0x65,0xeb);
#if defined(__cplusplus) && !defined(CINTERFACE)
MIDL_INTERFACE("320b7e22-3cb0-4cdf-8663-1d28910065eb")
ICryptographicBufferStatics : public IInspectable
{
    virtual HRESULT STDMETHODCALLTYPE Compare(
        IBuffer *object1,
        IBuffer *object2,
        boolean *isEqual) = 0;

    virtual HRESULT STDMETHODCALLTYPE GenerateRandom(
        UINT32 length,
        IBuffer **buffer) = 0;

    virtual HRESULT STDMETHODCALLTYPE GenerateRandomNumber(
        UINT32 *value) = 0;

    virtual HRESULT STDMETHODCALLTYPE CreateFromByteArray(
        UINT32 __valueSize,
        BYTE *value,
        IBuffer **buffer) = 0;

    virtual HRESULT STDMETHODCALLTYPE CopyToByteArray(
        IBuffer *buffer,
        UINT32 *__valueSize,
        BYTE **value) = 0;

    virtual HRESULT STDMETHODCALLTYPE DecodeFromHexString(
        HSTRING value,
        IBuffer **buffer) = 0;

    virtual HRESULT STDMETHODCALLTYPE EncodeToHexString(
        IBuffer *buffer,
        HSTRING *value) = 0;

    virtual HRESULT STDMETHODCALLTYPE DecodeFromBase64String(
        HSTRING value,
        IBuffer **buffer) = 0;

    virtual HRESULT STDMETHODCALLTYPE EncodeToBase64String(
        IBuffer *buffer,
        HSTRING *value) = 0;

    virtual HRESULT STDMETHODCALLTYPE ConvertStringToBinary(
        HSTRING value,
        BinaryStringEncoding encoding,
        IBuffer **buffer) = 0;

    virtual HRESULT STDMETHODCALLTYPE ConvertBinaryToString(
        BinaryStringEncoding encoding,
        IBuffer *buffer,
        HSTRING *value) = 0;

};
#ifdef __CRT_UUID_DECL
__CRT_UUID_DECL(ICryptographicBufferStatics, 0x320b7e22, 0x3cb0, 0x4cdf, 0x86,0x63, 0x1d,0x28,0x91,0x00,0x65,0xeb)
#endif
#else
typedef struct ICryptographicBufferStaticsVtbl {
    BEGIN_INTERFACE

    /*** IUnknown methods ***/
    HRESULT (STDMETHODCALLTYPE *QueryInterface)(
        ICryptographicBufferStatics* This,
        REFIID riid,
        void **ppvObject);

    ULONG (STDMETHODCALLTYPE *AddRef)(
        ICryptographicBufferStatics* This);

    ULONG (STDMETHODCALLTYPE *Release)(
        ICryptographicBufferStatics* This);

    /*** IInspectable methods ***/
    HRESULT (STDMETHODCALLTYPE *GetIids)(
        ICryptographicBufferStatics* This,
        ULONG *iidCount,
        IID **iids);

    HRESULT (STDMETHODCALLTYPE *GetRuntimeClassName)(
        ICryptographicBufferStatics* This,
        HSTRING *className);

    HRESULT (STDMETHODCALLTYPE *GetTrustLevel)(
        ICryptographicBufferStatics* This,
        TrustLevel *trustLevel);

    /*** ICryptographicBufferStatics methods ***/
    HRESULT (STDMETHODCALLTYPE *Compare)(
        ICryptographicBufferStatics* This,
        IBuffer *object1,
        IBuffer *object2,
        boolean *isEqual);

    HRESULT (STDMETHODCALLTYPE *GenerateRandom)(
        ICryptographicBufferStatics* This,
        UINT32 length,
        IBuffer **buffer);

    HRESULT (STDMETHODCALLTYPE *GenerateRandomNumber)(
        ICryptographicBufferStatics* This,
        UINT32 *value);

    HRESULT (STDMETHODCALLTYPE *CreateFromByteArray)(
        ICryptographicBufferStatics* This,
        UINT32 __valueSize,
        BYTE *value,
        IBuffer **buffer);

    HRESULT (STDMETHODCALLTYPE *CopyToByteArray)(
        ICryptographicBufferStatics* This,
        IBuffer *buffer,
        UINT32 *__valueSize,
        BYTE **value);

    HRESULT (STDMETHODCALLTYPE *DecodeFromHexString)(
        ICryptographicBufferStatics* This,
        HSTRING value,
        IBuffer **buffer);

    HRESULT (STDMETHODCALLTYPE *EncodeToHexString)(
        ICryptographicBufferStatics* This,
        IBuffer *buffer,
        HSTRING *value);

    HRESULT (STDMETHODCALLTYPE *DecodeFromBase64String)(
        ICryptographicBufferStatics* This,
        HSTRING value,
        IBuffer **buffer);

    HRESULT (STDMETHODCALLTYPE *EncodeToBase64String)(
        ICryptographicBufferStatics* This,
        IBuffer *buffer,
        HSTRING *value);

    HRESULT (STDMETHODCALLTYPE *ConvertStringToBinary)(
        ICryptographicBufferStatics* This,
        HSTRING value,
        BinaryStringEncoding encoding,
        IBuffer **buffer);

    HRESULT (STDMETHODCALLTYPE *ConvertBinaryToString)(
        ICryptographicBufferStatics* This,
        BinaryStringEncoding encoding,
        IBuffer *buffer,
        HSTRING *value);

    END_INTERFACE
} ICryptographicBufferStaticsVtbl;
interface ICryptographicBufferStatics {
    CONST_VTBL ICryptographicBufferStaticsVtbl* lpVtbl;
};

#ifdef COBJMACROS
#ifndef WIDL_C_INLINE_WRAPPERS
/*** IUnknown methods ***/
#define ICryptographicBufferStatics_QueryInterface(This,riid,ppvObject) (This)->lpVtbl->QueryInterface(This,riid,ppvObject)
#define ICryptographicBufferStatics_AddRef(This) (This)->lpVtbl->AddRef(This)
#define ICryptographicBufferStatics_Release(This) (This)->lpVtbl->Release(This)
/*** IInspectable methods ***/
#define ICryptographicBufferStatics_GetIids(This,iidCount,iids) (This)->lpVtbl->GetIids(This,iidCount,iids)
#define ICryptographicBufferStatics_GetRuntimeClassName(This,className) (This)->lpVtbl->GetRuntimeClassName(This,className)
#define ICryptographicBufferStatics_GetTrustLevel(This,trustLevel) (This)->lpVtbl->GetTrustLevel(This,trustLevel)
/*** ICryptographicBufferStatics methods ***/
#define ICryptographicBufferStatics_Compare(This,object1,object2,isEqual) (This)->lpVtbl->Compare(This,object1,object2,isEqual)
#define ICryptographicBufferStatics_GenerateRandom(This,length,buffer) (This)->lpVtbl->GenerateRandom(This,length,buffer)
#define ICryptographicBufferStatics_GenerateRandomNumber(This,value) (This)->lpVtbl->GenerateRandomNumber(This,value)
#define ICryptographicBufferStatics_CreateFromByteArray(This,__valueSize,value,buffer) (This)->lpVtbl->CreateFromByteArray(This,__valueSize,value,buffer)
#define ICryptographicBufferStatics_CopyToByteArray(This,buffer,__valueSize,value) (This)->lpVtbl->CopyToByteArray(This,buffer,__valueSize,value)
#define ICryptographicBufferStatics_DecodeFromHexString(This,value,buffer) (This)->lpVtbl->DecodeFromHexString(This,value,buffer)
#define ICryptographicBufferStatics_EncodeToHexString(This,buffer,value) (This)->lpVtbl->EncodeToHexString(This,buffer,value)
#define ICryptographicBufferStatics_DecodeFromBase64String(This,value,buffer) (This)->lpVtbl->DecodeFromBase64String(This,value,buffer)
#define ICryptographicBufferStatics_EncodeToBase64String(This,buffer,value) (This)->lpVtbl->EncodeToBase64String(This,buffer,value)
#define ICryptographicBufferStatics_ConvertStringToBinary(This,value,encoding,buffer) (This)->lpVtbl->ConvertStringToBinary(This,value,encoding,buffer)
#define ICryptographicBufferStatics_ConvertBinaryToString(This,encoding,buffer,value) (This)->lpVtbl->ConvertBinaryToString(This,encoding,buffer,value)
#else
/*** IUnknown methods ***/
static FORCEINLINE HRESULT ICryptographicBufferStatics_QueryInterface(ICryptographicBufferStatics* This,REFIID riid,void **ppvObject) {
    return This->lpVtbl->QueryInterface(This,riid,ppvObject);
}
static FORCEINLINE ULONG ICryptographicBufferStatics_AddRef(ICryptographicBufferStatics* This) {
    return This->lpVtbl->AddRef(This);
}
static FORCEINLINE ULONG ICryptographicBufferStatics_Release(ICryptographicBufferStatics* This) {
    return This->lpVtbl->Release(This);
}
/*** IInspectable methods ***/
static FORCEINLINE HRESULT ICryptographicBufferStatics_GetIids(ICryptographicBufferStatics* This,ULONG *iidCount,IID **iids) {
    return This->lpVtbl->GetIids(This,iidCount,iids);
}
static FORCEINLINE HRESULT ICryptographicBufferStatics_GetRuntimeClassName(ICryptographicBufferStatics* This,HSTRING *className) {
    return This->lpVtbl->GetRuntimeClassName(This,className);
}
static FORCEINLINE HRESULT ICryptographicBufferStatics_GetTrustLevel(ICryptographicBufferStatics* This,TrustLevel *trustLevel) {
    return This->lpVtbl->GetTrustLevel(This,trustLevel);
}
/*** ICryptographicBufferStatics methods ***/
static FORCEINLINE HRESULT ICryptographicBufferStatics_Compare(ICryptographicBufferStatics* This,IBuffer *object1,IBuffer *object2,boolean *isEqual) {
    return This->lpVtbl->Compare(This,object1,object2,isEqual);
}
static FORCEINLINE HRESULT ICryptographicBufferStatics_GenerateRandom(ICryptographicBufferStatics* This,UINT32 length,IBuffer **buffer) {
    return This->lpVtbl->GenerateRandom(This,length,buffer);
}
static FORCEINLINE HRESULT ICryptographicBufferStatics_GenerateRandomNumber(ICryptographicBufferStatics* This,UINT32 *value) {
    return This->lpVtbl->GenerateRandomNumber(This,value);
}
static FORCEINLINE HRESULT ICryptographicBufferStatics_CreateFromByteArray(ICryptographicBufferStatics* This,UINT32 __valueSize,BYTE *value,IBuffer **buffer) {
    return This->lpVtbl->CreateFromByteArray(This,__valueSize,value,buffer);
}
static FORCEINLINE HRESULT ICryptographicBufferStatics_CopyToByteArray(ICryptographicBufferStatics* This,IBuffer *buffer,UINT32 *__valueSize,BYTE **value) {
    return This->lpVtbl->CopyToByteArray(This,buffer,__valueSize,value);
}
static FORCEINLINE HRESULT ICryptographicBufferStatics_DecodeFromHexString(ICryptographicBufferStatics* This,HSTRING value,IBuffer **buffer) {
    return This->lpVtbl->DecodeFromHexString(This,value,buffer);
}
static FORCEINLINE HRESULT ICryptographicBufferStatics_EncodeToHexString(ICryptographicBufferStatics* This,IBuffer *buffer,HSTRING *value) {
    return This->lpVtbl->EncodeToHexString(This,buffer,value);
}
static FORCEINLINE HRESULT ICryptographicBufferStatics_DecodeFromBase64String(ICryptographicBufferStatics* This,HSTRING value,IBuffer **buffer) {
    return This->lpVtbl->DecodeFromBase64String(This,value,buffer);
}
static FORCEINLINE HRESULT ICryptographicBufferStatics_EncodeToBase64String(ICryptographicBufferStatics* This,IBuffer *buffer,HSTRING *value) {
    return This->lpVtbl->EncodeToBase64String(This,buffer,value);
}
static FORCEINLINE HRESULT ICryptographicBufferStatics_ConvertStringToBinary(ICryptographicBufferStatics* This,HSTRING value,BinaryStringEncoding encoding,IBuffer **buffer) {
    return This->lpVtbl->ConvertStringToBinary(This,value,encoding,buffer);
}
static FORCEINLINE HRESULT ICryptographicBufferStatics_ConvertBinaryToString(ICryptographicBufferStatics* This,BinaryStringEncoding encoding,IBuffer *buffer,HSTRING *value) {
    return This->lpVtbl->ConvertBinaryToString(This,encoding,buffer,value);
}
#endif
#endif

#endif

HRESULT STDMETHODCALLTYPE ICryptographicBufferStatics_Compare_Proxy(
    ICryptographicBufferStatics* This,
    IBuffer *object1,
    IBuffer *object2,
    boolean *isEqual);
void __RPC_STUB ICryptographicBufferStatics_Compare_Stub(
    IRpcStubBuffer* This,
    IRpcChannelBuffer* pRpcChannelBuffer,
    PRPC_MESSAGE pRpcMessage,
    DWORD* pdwStubPhase);
HRESULT STDMETHODCALLTYPE ICryptographicBufferStatics_GenerateRandom_Proxy(
    ICryptographicBufferStatics* This,
    UINT32 length,
    IBuffer **buffer);
void __RPC_STUB ICryptographicBufferStatics_GenerateRandom_Stub(
    IRpcStubBuffer* This,
    IRpcChannelBuffer* pRpcChannelBuffer,
    PRPC_MESSAGE pRpcMessage,
    DWORD* pdwStubPhase);
HRESULT STDMETHODCALLTYPE ICryptographicBufferStatics_GenerateRandomNumber_Proxy(
    ICryptographicBufferStatics* This,
    UINT32 *value);
void __RPC_STUB ICryptographicBufferStatics_GenerateRandomNumber_Stub(
    IRpcStubBuffer* This,
    IRpcChannelBuffer* pRpcChannelBuffer,
    PRPC_MESSAGE pRpcMessage,
    DWORD* pdwStubPhase);
HRESULT STDMETHODCALLTYPE ICryptographicBufferStatics_CreateFromByteArray_Proxy(
    ICryptographicBufferStatics* This,
    UINT32 __valueSize,
    BYTE *value,
    IBuffer **buffer);
void __RPC_STUB ICryptographicBufferStatics_CreateFromByteArray_Stub(
    IRpcStubBuffer* This,
    IRpcChannelBuffer* pRpcChannelBuffer,
    PRPC_MESSAGE pRpcMessage,
    DWORD* pdwStubPhase);
HRESULT STDMETHODCALLTYPE ICryptographicBufferStatics_CopyToByteArray_Proxy(
    ICryptographicBufferStatics* This,
    IBuffer *buffer,
    UINT32 *__valueSize,
    BYTE **value);
void __RPC_STUB ICryptographicBufferStatics_CopyToByteArray_Stub(
    IRpcStubBuffer* This,
    IRpcChannelBuffer* pRpcChannelBuffer,
    PRPC_MESSAGE pRpcMessage,
    DWORD* pdwStubPhase);
HRESULT STDMETHODCALLTYPE ICryptographicBufferStatics_DecodeFromHexString_Proxy(
    ICryptographicBufferStatics* This,
    HSTRING value,
    IBuffer **buffer);
void __RPC_STUB ICryptographicBufferStatics_DecodeFromHexString_Stub(
    IRpcStubBuffer* This,
    IRpcChannelBuffer* pRpcChannelBuffer,
    PRPC_MESSAGE pRpcMessage,
    DWORD* pdwStubPhase);
HRESULT STDMETHODCALLTYPE ICryptographicBufferStatics_EncodeToHexString_Proxy(
    ICryptographicBufferStatics* This,
    IBuffer *buffer,
    HSTRING *value);
void __RPC_STUB ICryptographicBufferStatics_EncodeToHexString_Stub(
    IRpcStubBuffer* This,
    IRpcChannelBuffer* pRpcChannelBuffer,
    PRPC_MESSAGE pRpcMessage,
    DWORD* pdwStubPhase);
HRESULT STDMETHODCALLTYPE ICryptographicBufferStatics_DecodeFromBase64String_Proxy(
    ICryptographicBufferStatics* This,
    HSTRING value,
    IBuffer **buffer);
void __RPC_STUB ICryptographicBufferStatics_DecodeFromBase64String_Stub(
    IRpcStubBuffer* This,
    IRpcChannelBuffer* pRpcChannelBuffer,
    PRPC_MESSAGE pRpcMessage,
    DWORD* pdwStubPhase);
HRESULT STDMETHODCALLTYPE ICryptographicBufferStatics_EncodeToBase64String_Proxy(
    ICryptographicBufferStatics* This,
    IBuffer *buffer,
    HSTRING *value);
void __RPC_STUB ICryptographicBufferStatics_EncodeToBase64String_Stub(
    IRpcStubBuffer* This,
    IRpcChannelBuffer* pRpcChannelBuffer,
    PRPC_MESSAGE pRpcMessage,
    DWORD* pdwStubPhase);
HRESULT STDMETHODCALLTYPE ICryptographicBufferStatics_ConvertStringToBinary_Proxy(
    ICryptographicBufferStatics* This,
    HSTRING value,
    BinaryStringEncoding encoding,
    IBuffer **buffer);
void __RPC_STUB ICryptographicBufferStatics_ConvertStringToBinary_Stub(
    IRpcStubBuffer* This,
    IRpcChannelBuffer* pRpcChannelBuffer,
    PRPC_MESSAGE pRpcMessage,
    DWORD* pdwStubPhase);
HRESULT STDMETHODCALLTYPE ICryptographicBufferStatics_ConvertBinaryToString_Proxy(
    ICryptographicBufferStatics* This,
    BinaryStringEncoding encoding,
    IBuffer *buffer,
    HSTRING *value);
void __RPC_STUB ICryptographicBufferStatics_ConvertBinaryToString_Stub(
    IRpcStubBuffer* This,
    IRpcChannelBuffer* pRpcChannelBuffer,
    PRPC_MESSAGE pRpcMessage,
    DWORD* pdwStubPhase);

#endif  /* __ICryptographicBufferStatics_INTERFACE_DEFINED__ */

/* Begin additional prototypes for all interfaces */

ULONG           __RPC_USER HSTRING_UserSize     (ULONG *, ULONG, HSTRING *);
unsigned char * __RPC_USER HSTRING_UserMarshal  (ULONG *, unsigned char *, HSTRING *);
unsigned char * __RPC_USER HSTRING_UserUnmarshal(ULONG *, unsigned char *, HSTRING *);
void            __RPC_USER HSTRING_UserFree     (ULONG *, HSTRING *);

/* End additional prototypes */

#ifdef __cplusplus
}
#endif

#endif /* __windows_security_cryptography_h__ */
