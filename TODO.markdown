## TODO

### Password strength

Any password that is stored in the database needs to be of sufficient
entropy. The password therefore needs to be validated before it is stored.

### Forgot password

The user should be able to retrieve access to an account if he has forgotten
his password but remembers his registered email address. When such a request
is received, the server should email the user a link to a page that lets the
user choose a new password. This link should contain a large access token
that is valid for a short period of time. Only a hashed and salted (using
BCrypt) version of the token should be stored on the server.

### Session storage

At the moment, session data is stored hashed using SHA-256 in the database.
Ideally, the data should be hashed and salted using BCrypt.

### Database services

The following features need to be implemented in the database with associated 
API as webservices, accessible by the app.

#### Groups

Users of the app should be able to create "groups" relevant to specific subjects,
university programmes, courses, lectures, group projects, etc..

#### Asking questions

Users should be able to ask questions to groups and other members of the groups
should be able to answer these questions. They should be able to up-vote answers
that they think are helpful and down-vote unhelpful answers.

#### Discussions

Users should be able to create discussion threads within groups.
