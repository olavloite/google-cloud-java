// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/firestore/admin/v1/firestore_admin.proto

package com.google.firestore.admin.v1;

public final class FirestoreAdminProto {
  private FirestoreAdminProto() {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_firestore_admin_v1_CreateIndexRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_firestore_admin_v1_CreateIndexRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_firestore_admin_v1_ListIndexesRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_firestore_admin_v1_ListIndexesRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_firestore_admin_v1_ListIndexesResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_firestore_admin_v1_ListIndexesResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_firestore_admin_v1_GetIndexRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_firestore_admin_v1_GetIndexRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_firestore_admin_v1_DeleteIndexRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_firestore_admin_v1_DeleteIndexRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_firestore_admin_v1_UpdateFieldRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_firestore_admin_v1_UpdateFieldRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_firestore_admin_v1_GetFieldRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_firestore_admin_v1_GetFieldRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_firestore_admin_v1_ListFieldsRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_firestore_admin_v1_ListFieldsRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_firestore_admin_v1_ListFieldsResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_firestore_admin_v1_ListFieldsResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_firestore_admin_v1_ExportDocumentsRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_firestore_admin_v1_ExportDocumentsRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_firestore_admin_v1_ImportDocumentsRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_firestore_admin_v1_ImportDocumentsRequest_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

  static {
    java.lang.String[] descriptorData = {
      "\n/google/firestore/admin/v1/firestore_ad"
          + "min.proto\022\031google.firestore.admin.v1\032\034go"
          + "ogle/api/annotations.proto\032%google/fires"
          + "tore/admin/v1/field.proto\032%google/firest"
          + "ore/admin/v1/index.proto\032#google/longrun"
          + "ning/operations.proto\032\033google/protobuf/e"
          + "mpty.proto\032 google/protobuf/field_mask.p"
          + "roto\"U\n\022CreateIndexRequest\022\016\n\006parent\030\001 \001"
          + "(\t\022/\n\005index\030\002 \001(\0132 .google.firestore.adm"
          + "in.v1.Index\"[\n\022ListIndexesRequest\022\016\n\006par"
          + "ent\030\001 \001(\t\022\016\n\006filter\030\002 \001(\t\022\021\n\tpage_size\030\003"
          + " \001(\005\022\022\n\npage_token\030\004 \001(\t\"a\n\023ListIndexesR"
          + "esponse\0221\n\007indexes\030\001 \003(\0132 .google.firest"
          + "ore.admin.v1.Index\022\027\n\017next_page_token\030\002 "
          + "\001(\t\"\037\n\017GetIndexRequest\022\014\n\004name\030\001 \001(\t\"\"\n\022"
          + "DeleteIndexRequest\022\014\n\004name\030\001 \001(\t\"v\n\022Upda"
          + "teFieldRequest\022/\n\005field\030\001 \001(\0132 .google.f"
          + "irestore.admin.v1.Field\022/\n\013update_mask\030\002"
          + " \001(\0132\032.google.protobuf.FieldMask\"\037\n\017GetF"
          + "ieldRequest\022\014\n\004name\030\001 \001(\t\"Z\n\021ListFieldsR"
          + "equest\022\016\n\006parent\030\001 \001(\t\022\016\n\006filter\030\002 \001(\t\022\021"
          + "\n\tpage_size\030\003 \001(\005\022\022\n\npage_token\030\004 \001(\t\"_\n"
          + "\022ListFieldsResponse\0220\n\006fields\030\001 \003(\0132 .go"
          + "ogle.firestore.admin.v1.Field\022\027\n\017next_pa"
          + "ge_token\030\002 \001(\t\"Y\n\026ExportDocumentsRequest"
          + "\022\014\n\004name\030\001 \001(\t\022\026\n\016collection_ids\030\002 \003(\t\022\031"
          + "\n\021output_uri_prefix\030\003 \001(\t\"X\n\026ImportDocum"
          + "entsRequest\022\014\n\004name\030\001 \001(\t\022\026\n\016collection_"
          + "ids\030\002 \003(\t\022\030\n\020input_uri_prefix\030\003 \001(\t2\205\014\n\016"
          + "FirestoreAdmin\022\252\001\n\013CreateIndex\022-.google."
          + "firestore.admin.v1.CreateIndexRequest\032\035."
          + "google.longrunning.Operation\"M\202\323\344\223\002G\">/v"
          + "1/{parent=projects/*/databases/*/collect"
          + "ionGroups/*}/indexes:\005index\022\264\001\n\013ListInde"
          + "xes\022-.google.firestore.admin.v1.ListInde"
          + "xesRequest\032..google.firestore.admin.v1.L"
          + "istIndexesResponse\"F\202\323\344\223\002@\022>/v1/{parent="
          + "projects/*/databases/*/collectionGroups/"
          + "*}/indexes\022\240\001\n\010GetIndex\022*.google.firesto"
          + "re.admin.v1.GetIndexRequest\032 .google.fir"
          + "estore.admin.v1.Index\"F\202\323\344\223\002@\022>/v1/{name"
          + "=projects/*/databases/*/collectionGroups"
          + "/*/indexes/*}\022\234\001\n\013DeleteIndex\022-.google.f"
          + "irestore.admin.v1.DeleteIndexRequest\032\026.g"
          + "oogle.protobuf.Empty\"F\202\323\344\223\002@*>/v1/{name="
          + "projects/*/databases/*/collectionGroups/"
          + "*/indexes/*}\022\237\001\n\010GetField\022*.google.fires"
          + "tore.admin.v1.GetFieldRequest\032 .google.f"
          + "irestore.admin.v1.Field\"E\202\323\344\223\002?\022=/v1/{na"
          + "me=projects/*/databases/*/collectionGrou"
          + "ps/*/fields/*}\022\257\001\n\013UpdateField\022-.google."
          + "firestore.admin.v1.UpdateFieldRequest\032\035."
          + "google.longrunning.Operation\"R\202\323\344\223\002L2C/v"
          + "1/{field.name=projects/*/databases/*/col"
          + "lectionGroups/*/fields/*}:\005field\022\260\001\n\nLis"
          + "tFields\022,.google.firestore.admin.v1.List"
          + "FieldsRequest\032-.google.firestore.admin.v"
          + "1.ListFieldsResponse\"E\202\323\344\223\002?\022=/v1/{paren"
          + "t=projects/*/databases/*/collectionGroup"
          + "s/*}/fields\022\241\001\n\017ExportDocuments\0221.google"
          + ".firestore.admin.v1.ExportDocumentsReque"
          + "st\032\035.google.longrunning.Operation\"<\202\323\344\223\002"
          + "6\"1/v1/{name=projects/*/databases/*}:exp"
          + "ortDocuments:\001*\022\241\001\n\017ImportDocuments\0221.go"
          + "ogle.firestore.admin.v1.ImportDocumentsR"
          + "equest\032\035.google.longrunning.Operation\"<\202"
          + "\323\344\223\0026\"1/v1/{name=projects/*/databases/*}"
          + ":importDocuments:\001*B\301\001\n\035com.google.fires"
          + "tore.admin.v1B\023FirestoreAdminProtoP\001Z>go"
          + "ogle.golang.org/genproto/googleapis/fire"
          + "store/admin/v1;admin\242\002\004GCFS\252\002\037Google.Clo"
          + "ud.Firestore.Admin.V1\312\002\037Google\\Cloud\\Fir"
          + "estore\\Admin\\V1b\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
        descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.api.AnnotationsProto.getDescriptor(),
          com.google.firestore.admin.v1.FieldProto.getDescriptor(),
          com.google.firestore.admin.v1.IndexProto.getDescriptor(),
          com.google.longrunning.OperationsProto.getDescriptor(),
          com.google.protobuf.EmptyProto.getDescriptor(),
          com.google.protobuf.FieldMaskProto.getDescriptor(),
        },
        assigner);
    internal_static_google_firestore_admin_v1_CreateIndexRequest_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_google_firestore_admin_v1_CreateIndexRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_firestore_admin_v1_CreateIndexRequest_descriptor,
            new java.lang.String[] {
              "Parent", "Index",
            });
    internal_static_google_firestore_admin_v1_ListIndexesRequest_descriptor =
        getDescriptor().getMessageTypes().get(1);
    internal_static_google_firestore_admin_v1_ListIndexesRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_firestore_admin_v1_ListIndexesRequest_descriptor,
            new java.lang.String[] {
              "Parent", "Filter", "PageSize", "PageToken",
            });
    internal_static_google_firestore_admin_v1_ListIndexesResponse_descriptor =
        getDescriptor().getMessageTypes().get(2);
    internal_static_google_firestore_admin_v1_ListIndexesResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_firestore_admin_v1_ListIndexesResponse_descriptor,
            new java.lang.String[] {
              "Indexes", "NextPageToken",
            });
    internal_static_google_firestore_admin_v1_GetIndexRequest_descriptor =
        getDescriptor().getMessageTypes().get(3);
    internal_static_google_firestore_admin_v1_GetIndexRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_firestore_admin_v1_GetIndexRequest_descriptor,
            new java.lang.String[] {
              "Name",
            });
    internal_static_google_firestore_admin_v1_DeleteIndexRequest_descriptor =
        getDescriptor().getMessageTypes().get(4);
    internal_static_google_firestore_admin_v1_DeleteIndexRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_firestore_admin_v1_DeleteIndexRequest_descriptor,
            new java.lang.String[] {
              "Name",
            });
    internal_static_google_firestore_admin_v1_UpdateFieldRequest_descriptor =
        getDescriptor().getMessageTypes().get(5);
    internal_static_google_firestore_admin_v1_UpdateFieldRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_firestore_admin_v1_UpdateFieldRequest_descriptor,
            new java.lang.String[] {
              "Field", "UpdateMask",
            });
    internal_static_google_firestore_admin_v1_GetFieldRequest_descriptor =
        getDescriptor().getMessageTypes().get(6);
    internal_static_google_firestore_admin_v1_GetFieldRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_firestore_admin_v1_GetFieldRequest_descriptor,
            new java.lang.String[] {
              "Name",
            });
    internal_static_google_firestore_admin_v1_ListFieldsRequest_descriptor =
        getDescriptor().getMessageTypes().get(7);
    internal_static_google_firestore_admin_v1_ListFieldsRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_firestore_admin_v1_ListFieldsRequest_descriptor,
            new java.lang.String[] {
              "Parent", "Filter", "PageSize", "PageToken",
            });
    internal_static_google_firestore_admin_v1_ListFieldsResponse_descriptor =
        getDescriptor().getMessageTypes().get(8);
    internal_static_google_firestore_admin_v1_ListFieldsResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_firestore_admin_v1_ListFieldsResponse_descriptor,
            new java.lang.String[] {
              "Fields", "NextPageToken",
            });
    internal_static_google_firestore_admin_v1_ExportDocumentsRequest_descriptor =
        getDescriptor().getMessageTypes().get(9);
    internal_static_google_firestore_admin_v1_ExportDocumentsRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_firestore_admin_v1_ExportDocumentsRequest_descriptor,
            new java.lang.String[] {
              "Name", "CollectionIds", "OutputUriPrefix",
            });
    internal_static_google_firestore_admin_v1_ImportDocumentsRequest_descriptor =
        getDescriptor().getMessageTypes().get(10);
    internal_static_google_firestore_admin_v1_ImportDocumentsRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_firestore_admin_v1_ImportDocumentsRequest_descriptor,
            new java.lang.String[] {
              "Name", "CollectionIds", "InputUriPrefix",
            });
    com.google.protobuf.ExtensionRegistry registry =
        com.google.protobuf.ExtensionRegistry.newInstance();
    registry.add(com.google.api.AnnotationsProto.http);
    com.google.protobuf.Descriptors.FileDescriptor.internalUpdateFileDescriptor(
        descriptor, registry);
    com.google.api.AnnotationsProto.getDescriptor();
    com.google.firestore.admin.v1.FieldProto.getDescriptor();
    com.google.firestore.admin.v1.IndexProto.getDescriptor();
    com.google.longrunning.OperationsProto.getDescriptor();
    com.google.protobuf.EmptyProto.getDescriptor();
    com.google.protobuf.FieldMaskProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
