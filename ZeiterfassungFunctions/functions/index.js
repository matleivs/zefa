const functions = require('firebase-functions'); // Cloud Functions schreiben und Trigger Regeln erstellen
const admin = require('firebase-admin'); // Firebase Platform auf einem Server mit Admin-Zugriff nutzen, also *in die RT Database schreiben (Write), *FCM Notifications versenden, ...
admin.initializeApp();


exports.addWorkday = functions.https.onCall((data, context) => {

    const date = data.date; // date of workday, passed from Android Client App
    const startTime = data.startTime; // startTime of workingday, passed from Android Client App
    const description = data.description; // description of what did you work on this working day

    // [START HttpsErrors]
    // checking attributes are there
    if (date.length === 0 || startTime.length === 0) {
        throw new functions.https.HttpsError('failed-precondition',
            'The function should be called with a date and timestamp, if none set by user, use default.')
    }

    // checking user is authenticated
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated',
            'Function must be called while authenticated, to assign the data to the User.')
    }
    // [END HttpsErrors]

    // [START authIntegration]
    // Auth/ User information is automatically added to the request
    const uid = context.auth.uid;
    // Note: When you pass an object to Firebase, the values of the properties
    // can be a value or null (in which case the property will be removed). 
    // They can not be undefined, this will result in an error.
    const name = context.auth.token.name || null;
    const email = context.auth.email || null;
    const picture = context.auth.token.picture || null;
    // [END authIntegration]

    // Saving the new workday instance to the Realtime Database.
   // WIP: Do something with it
    return admin.database().ref('/workday').push({
        date: date,
        startTime: startTime,
        user: { uid, name, picture, email },
    }).then(() => {
        console.log('New Workday was added');
        // Returning the info of the newly added workday to the client.
        return {
            date: date,
            startTime: startTime,
            description: description
        };
    })
        // [END returnMessageAsync]
        .catch((error) => {
            // Re-throwing the error as an HttpsError so that the client gets the error details.
            throw new functions.https.HttpsError('unknown', error.message, error);
        });
})