// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/cloud/talent/v4beta1/profile.proto

package com.google.cloud.talent.v4beta1;

public interface EmailOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.cloud.talent.v4beta1.Email)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Optional.
   * The usage of the email address. For example, SCHOOL, WORK, PERSONAL.
   * </pre>
   *
   * <code>.google.cloud.talent.v4beta1.ContactInfoUsage usage = 1;</code>
   */
  int getUsageValue();
  /**
   *
   *
   * <pre>
   * Optional.
   * The usage of the email address. For example, SCHOOL, WORK, PERSONAL.
   * </pre>
   *
   * <code>.google.cloud.talent.v4beta1.ContactInfoUsage usage = 1;</code>
   */
  com.google.cloud.talent.v4beta1.ContactInfoUsage getUsage();

  /**
   *
   *
   * <pre>
   * Optional.
   * Email address.
   * Number of characters allowed is 4,000.
   * </pre>
   *
   * <code>string email_address = 2;</code>
   */
  java.lang.String getEmailAddress();
  /**
   *
   *
   * <pre>
   * Optional.
   * Email address.
   * Number of characters allowed is 4,000.
   * </pre>
   *
   * <code>string email_address = 2;</code>
   */
  com.google.protobuf.ByteString getEmailAddressBytes();
}
