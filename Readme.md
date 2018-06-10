# Introduction
This project provides an example of API using Akka Http and Akka Actors.

The API contains basic calls for making transactions between users. For achieving that there are 3 different endpoints:

* Users: contains basic operations to add, get, delete and list users. The users contain a organization section.

    The organization section it is the tricky part. It wouldnt be good to store the organisation as part of the UserDomain, as any update in the organization would require to update all the users that belong to that organization.

* Usage

        sbt docker:publishLocal
        docker run --rm -p8080:8080 users-api:0.0.1-SNAPSHOT

* Endpoints

    * GET /users  -> retrieves a list of all the users
    * GET /users/${ID} -> retrieves the user with the ID
    * POST /users  -> stores a new User. Here there are different possibilities:
        - New User and New Organization   -> Returns back a User with the userId and organizationId generated
        - New User and Organization Update -> returns back a User with the userId generated and it update the organization on the store.
        - User with a value in the ID -> Not permited
    * PUT /users -> Update an existing user. Possible values
        - Sucessful 200
        - Error 404
    * DELETE /users/${ID} -> Possible values
        - Sucessful 200
        - Error 404

* Code explanation

 Basically all the logic is in the FullUserService. It manages the futures of the calls to the OrganisationStore and UserStore.

 The OrganisationStore and UserStore logic is in a GenericStoreService. I created this class with all the common methods for a CRUD Store, that is storing an UUIDable record. Thats why, User and Organisation case classes extends UUIDable.

 All the important domain can be found in the Domain object.

 Spray considerations. It couldnt be used format5 and format8 in the Organisation and User case classes to serialize and deserialize.
 As there was an attribute in the json output format named "type", I had to do a custom relationship between the json attributes and the case classes. (Check the jsonSupport object)

* Postman

 All the endpoints have been tested by the Scala Spec and as well using postman. The postman file has been included. It can be imported and tried.
