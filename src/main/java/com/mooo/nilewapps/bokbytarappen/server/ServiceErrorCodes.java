/**
 *  Copyright 2013 Robert Welin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mooo.nilewapps.bokbytarappen.server;

/**
 * Defines error codes for the web service.
 */
class ServiceErrorCodes {

  /**
   * Returned when the user tries to register a password, but
   * it has too low guessing entropy.
   */
  public static final int BAD_PASSWORD = 1;

  /**
   * Returned when the user tries to register an email address, but
   * another user has already registered the address.
   */
  public static final int UNAVAILABLE_EMAIL = 2;

  /**
   * Returned when the user tries to register an invalid email address.
   */
  public static final int INVALID_EMAIL = 3;

  /**
   * Returned when the user tries to join a Group that doesn't exist.
   */
  public static final int NON_EXISTING_GROUP = 4;

  /**
   * Returned when the user tries to join a Group which parent the
   * user isn't a member of.
   */
  public static final int NOT_MEMBER_OF_PARENT_GROUP = 5;

  /**
   * Returned when the user tries to join a Group which the user is
   * already a member of.
   */
  public static final int ALREADY_MEMBER_OF_GROUP = 6;

  /**
   * Returned when the user tries to leave a Group which the user is
   * not a member of.
   */
  public static final int NOT_MEMBER_OF_GROUP = 7;

  /**
   * Returned when the user tries to leave a Group when the user is
   * still a member of a child Group.
   */
  public static final int MEMBER_OF_CHILD_GROUP = 8;

  /**
   * Returned when the user tries to submit a post with a title that
   * is too short.
   */
  public static final int TITLE_TOO_SHORT = 9;

  /**
   * Returned when the user tries to submit a post with content text
   * that is too short.
   */
  public static final int CONTENT_TOO_SHORT = 10;

  /**
   * Returned when the user tries to respond to a message that doesn't
   * exist.
   */
  public static final int RESPONSE_TO_NOTHING = 11;
}
