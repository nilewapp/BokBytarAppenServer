## TODO

### Password strength

The strength of a password is now checked before it is stored, however
the method is somewhat crude (essentially scaled Shannon entropy). This
should be improved.

### Regular clean-up

Temporary data such as sessions should be regularly deleted from the database.

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
