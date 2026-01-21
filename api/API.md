# API – JitSUSmon Chat
Protocol: HTTPS and JSON.
A simple authentication uses a `session_id` cookie containing the user's ID.

## Schemas
- **User**: `{ "userId": number, "username": string }`
- **Message**: `{ "userId": number, "msgId": number, "content": string }`
>Extended user fields are only available on specific endpoints and only in server-side.

---

## Features
- [User management](#users-endpoints)
  - [Create a new user](#create-a-new-user)
  - [Edit a user](#edit-a-user)
  - [List users](#list-users)
  - [Get one user](#get-one-user)
  - [Delete a user](#delete-a-user)
- [Authentication / Session](#authentication--session)
  - [Login](#login)
  - [Logout](#logout)
  - [Profile](#profile)
- [Messages Endpoints](#messages-endpoints)
  - [Post a message](#post-a-message)
  - [Edit a message](#edit-a-message)
  - [List my messages](#list-my-messages)
  - [List all messages](#list-all-messages)
  - [Remove a message](#remove-a-message)
- [Implementation notes](#implementation-notes)

---

## Users Endpoints

### Create a new user
- `POST /users`

Create a new user.
After creating a user, the client must log in to obtain the `session_id` cookie before performing other actions.

#### Request
The request body must contain a JSON body with the following properties:
- `username` - the requested username
- `email` - the requested user email
- `password` - the requested user password

##### command line example:
~~~bash
curl -X POST https://jitsusmon.duckdns.org/users \
  -H "Content-Type: application/json" \
  -d '{"username":"MasterMax","email":"max.muster@exemple.ch","password":"securePassword123"}'
~~~

#### Response
The response body contains a JSON object with the following properties:
- `userId` - The unique identifier of the user.
- `username` - The name of the user.
- `email` - The email address of the user.

**Exemple:**
~~~json
{
  "userId": 1, 
  "username": "MasterMax",
  "email": "max.muster@exemple.ch"
}
~~~

#### Status codes
- `201` Created - user created successfully
- `400` Bad Request - invalid body
- `409` Conflict - user already exists

---

### Edit a user
- `PATCH /users/{userId}`

Edit the user with the given ID.

#### Request
Requires `session_id` cookie. Only the logged-in user can edit their own information.
>If the `userId` in the URL does not match the `session_id` cookie, the server returns `403 Forbidden`.

The request body must contain a JSON body with the following properties (one or more):
- `email` - the new email
- `password` - the new password

##### command line example:
~~~bash
curl -b cookie.txt -X PATCH https://jitsusmon.duckdns.org/users/1 \
  -H "Content-Type: application/json" \
  -d '{"email":"new.email@exemple.com"}'
~~~
>All provided fields are updated at once.

#### Response
The response body contains a JSON object with the updated user properties:
- `userId` - The unique identifier of the user.
- `username` - The name of the user.
- `email` - The email address of the user.

**Exemple:**
~~~json
{
  "userId": 1,
  "username": "MasterMax",
  "email": "new.email@exemple.com"
}
~~~

#### Status codes
- `200` OK - user updated successfully
- `400` Bad Request - invalid body
- `401` Unauthorized - user not logged in
- `403` Forbidden - cannot edit another user
- `404` Not Found - user does not exist

---

### List users
- `GET /users`

Return the full list of users, with optional filtering by name.

#### Query parameters (optional)
- `username` - filter users by username (exact match)

**Example:**
- `GET /users?username=MasterMax`

#### Response
The response body contains a JSON array of user objects contains the following properties:
- `userId` - The unique identifier of the user.
- `username` - The name of the user.

no more details are provided for privacy reasons.

**Exemple:**
~~~json  
[  
    {
      "userId": 1,
      "username": "MasterMax"
    },
    {
      "userId": 2,
      "username": "Bob"
    }
]  
~~~  

#### Status codes
- `200` OK - success
- `204` No Content - success, but zero result
- `400` Bad Request - invalid query parameter

#### command line example:
~~~bash
curl -b cookie.txt -x GET https://jitsusmon.duckdns.org/users?username=MasterMax
~~~

---  

### Get one user
- `GET /users/{userId}`

Get one user by its ID number.

#### Request
The request path must contain the ID of the user.

#### Response
The response body contains a JSON object contains the following properties:
- `userId` - The unique identifier of the user.
- `username` - The name of the user.

no more details are provided for privacy reasons.

**Exemple:**
~~~json  
{  
  "userId": 1,
  "username": "MasterMax"
}  
~~~  

#### Status codes
- `200` OK - success
- `400` Bad Request - invalid query parameter
- `404` Not Found - user does not exist

##### command line example:
~~~bash
curl -b cookie.txt -X GET https://jitsusmon.duckdns.org/users/1
~~~

---  

### Delete a user
- `DELETE /users/{userId}`

Delete the user with the given ID.
This operation also affects messages, as all messages sent by this user are deleted.

The session cookie (`session_id`) is also removed.

#### Request
Requires `session_id` cookie.
Only the logged-in user can delete their own account.
>If the `userId` in the URL does not match the `session_id` cookie, the server returns `403 Forbidden`.

No additional confirmation is required.
#### Response
An empty body, and the `session_id` cookie is removed.

#### Status codes
- `204` No Content  - account closed with success
- `401` Unauthorized - user not logged in
- `403` Forbidden - cannot delete another user
- `404` Not Found - user does not exist

##### command line example:
~~~bash
curl -b cookie.txt -X DELETE https://jitsusmon.duckdns.org/users/1
~~~

---

## Authentication / Session

### Login
- `POST /login`

Authenticate a user. Sets a `session_id` cookie with the user's ID.

#### Request
Accept a JSON body with the following properties:
- `username` or `email` - the username - the user email
- `password` - the user password
~~~json  
{  
  "username": "MasterMax", 
  "password": "securePassword123"
}  
~~~  

or

~~~json
{
  "email": "max.muster@exemple.ch", 
  "password": "securePassword123"
}  
~~~

#### Response
No response body.

On success, a `session_id` session cookie is set.
> If the credentials are invalid, no cookie is set.
> If the user is already logged in (valid `session_id` cookie), the server returns `400 Bad Request`.

#### Status codes
- `204` No Content - success
- `400` Bad Request - invalid body or already logged in
- `401` Unauthorized - invalid credentials

##### command line example:
~~~bash
curl -c cookie.txt -X POST https://jitsusmon.duckdns.org/login \
  -H "Content-Type: application/json" \
  -d '{"username":"MasterMax","password":"securePassword123"}'
~~~

---  

### Logout
- `POST /logout`

Logout the current user.

#### Request
Empty body.
Requires a valid `session_id` session cookie.
>Requests without this cookie return `401 Unauthorized`.

#### Response
The response body is empty, and the `session_id` cookie is removed.

#### Status codes
- `204` No Content - success
- `401` Unauthorized - no user logged in

##### command line example:
~~~bash
curl -b cookie.txt -X POST https://jitsusmon.duckdns.org/logout
~~~

---

### Profile
- `GET /profile`

Return the currently logged-in user (based on the `session_id` cookie).

#### Request
Requires a valid `session_id` session cookie.
>Requests without this cookie return `401 Unauthorized`.


#### Response
The response body contains a JSON object with the following properties:
- `userId` - The unique identifier of the user.
- `username` - The name of the user.
~~~json  
{
  "userId": 1,
  "username": "MasterMax"
}  
~~~  

#### Status codes
- `200` OK - success
- `401` Unauthorized - no user logged in

##### command line example:
~~~bash
curl -b cookie.txt -X GET https://jitsusmon.duckdns.org/profile
~~~

---

## Messages Endpoints

### Post a message
- `POST /messages`

Post a new message as the logged-in user.

#### Request
Requires a valid `session_id` session cookie.
>Requests without this cookie return `401 Unauthorized`.

JSON body:
- `content` - message text

Example:
~~~json
{
  "content": "Hello"
}
~~~

#### Response
JSON object example:
~~~json
{
	"userId": 1,
	"msgId": 42, 
	"content": "Hello"
}
~~~

#### Status codes
- `201` Created - message posted successfully
- `400` Bad Request - invalid body
- `401` Unauthorized - not logged in

##### command line example:
~~~bash
curl -b cookie.txt -X POST https://jitsusmon.duckdns.org/messages \
  -H "Content-Type: application/json" \
  -d '{"content":"Hello"}'
~~~

---

### Edit a message
- `PATCH /messages/{msgId}`

Edit a message if the requester is the owner.

#### Request
Requires a valid `session_id` session cookie.
> Requests without this cookie return `401 Unauthorized`.
> If the logged-in user is not the owner of the message, the server returns `403 Forbidden`.

JSON body:
- `content` - new text

Example:
~~~json
{
  "content": "Updated text"
}  
~~~

#### Response
JSON object response example:
~~~json  
{
  "msgId": 42,
  "content": "Updated text"
}
~~~  

#### Status codes
- `200` OK - message updated successfully
- `400` Bad Request - invalid body
- `401` Unauthorized - not logged in
- `403` Forbidden - not the owner of the message
- `404` Not Found - message does not exist

##### command line example:
~~~bash
curl -b cookie.txt -X PATCH https://jitsusmon.duckdns.org/messages/42 \
  -H "Content-Type: application/json" \
  -d '{"content":"New Text"}'
~~~

---

### List my messages
- `GET /messages/mine`

Return all messages of the logged-in user (use of the `session_id` cookie).

#### Request
Requires `session_id` cookie.
> Requests without this cookie return `401 Unauthorized`.

#### Response
The server sent a JSON structure with this structure.

~~~json
[
  {
    "msgId": 42,
    "postedDate": "2024-10-01T12:34:56Z",
    "modifiedDate": "2024-10-02T14:20:00Z",
    "content": "Hello"
  },
  {
    "msgId": 45,
    "postedDate": "2024-10-03T09:15:30Z",
    "modifiedDate": null,
	"content": "Hello"
  }
]
~~~

#### Status codes
- `200` OK - success
- `401` Unauthorized - not logged in

##### command line example:
~~~bash
curl -b cookie.txt -X GET https://jitsusmon.duckdns.org/messages/mine
~~~

---

### List all messages
- `GET /messages`

Return all messages. Optional query parameter:
- `username` - filter by username
- `sinceDate` - with this format : `dd-mm-yyyy`

Example:
- `GET /messages?username=MasterMax`
- `GET /messages?sinceDate=01-01-2026`

#### Request
Requires `session_id` cookie.
>Requests without this cookie return `401 Unauthorized`.

#### Response
The server sent a JSON array of message objects with this structure.

~~~json
[  
  {
    "username": "MasterMax",
    "postedDate": "2024-10-01T12:34:56Z",
    "modifiedDate": "2024-10-02T14:20:00Z",
	"content": "Hello"
  },
  {
	"username": "H4ckB01",
    "postedDate": "2024-11-05T10:00:00Z",
    "modifiedDate": null,
	"content": "Ces tests qu'envoie l'école sont vraiment embêtants, ils ne font que stresser le destinataire. Et ils ne retiennent pas son but de prévention"
  }
]
~~~
> Note: this response format is specific to this endpoint and differs from other message listings.

#### Status codes
- `200` OK
- `304` Not Modified - when using cache with `If-Modified-Since` header
- `400` Bad Request - invalid query parameter
- `401` Unauthorized - not logged in
- `404` Not Found - user not found (when filtering by username)

##### command line example:
**With query parameter: (without cache)**
~~~bash
curl -b cookie.txt -X GET https://jitsusmon.duckdns.org/messages?username=MasterMax
~~~
**With cache:**
~~~bash
curl -b cookie.txt \
  -H 'If-Modified-Since: Tue, 01 Oct 2024 12:34:56 GMT' \
  -X GET https://jitsusmon.duckdns.org/messages
~~~
---

### Remove a message
- `DELETE /messages/{msgId}`

Delete a message if the requester is the owner.

#### Request
Requires `session_id` cookie.

#### Response
204 No Content

#### Status codes
- `204` No Content - message deleted successfully
- `401` Unauthorized - not logged in
- `403` Forbidden - not the owner of the message
- `404` Not Found - message does not exist

##### command line example:
~~~bash
curl -b cookie.txt -X DELETE https://jitsusmon.duckdns.org/messages/2
~~~

---

## Implementation notes
- Two conceptual tables:
  - `User (UserId | Name | Role)`
    - `Role`: `USER` / `ADMIN` (only `ADMIN` can delete/edit other users `yet not implemented`)
      - default: `USER`
      - Server-side only, not exposed in API and cannot be modified via API
  - `Message (UserId | MsgId | postedDate | modifiedDate |Content)`
    - `postedDate`: automatically set when the message is posted
      - Server-side only, not provided by client and cannot be modified via API
    - `modifiedDate`: automatically set when modified, if not, value: `null`
      - Server-side only, not provided by client and automatically updated by server when message is edited

- Communication via HTTPS and JSON.
- Errors return a minimal JSON body:

~~~json
{
  "error": "message"
}
~~~