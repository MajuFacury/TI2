import com.microsoft.azure.cognitiveservices.vision.faceapi.*;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.*;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class FaceQuickstart {

    public static void main(String[] args) {


        final String SINGLE_FACE_URL = "https://www.biography.com/.image/t_share/MTQ1MzAyNzYzOTgxNTE0NTEz/john-f-kennedy---mini-biography.jpg";
        final String SINGLE_IMAGE_NAME = 
                SINGLE_FACE_URL.substring(SINGLE_FACE_URL.lastIndexOf('/')+1, SINGLE_FACE_URL.length());
        
        final String  GROUP_FACES_URL = "http://www.historyplace.com/kennedy/president-family-portrait-closeup.jpg";
        final String GROUP_IMAGE_NAME = 
                GROUP_FACES_URL.substring(GROUP_FACES_URL.lastIndexOf('/')+1, GROUP_FACES_URL.length());

        final String IMAGE_BASE_URL = "https://csdx.blob.core.windows.net/resources/Face/Images/";

        final String PERSON_GROUP_ID = "my-families"; // can be any lowercase, 0-9, "-", or "_" character.
        final String FACE_LIST_ID = "my-families-list";

     
        final String KEY = "PASTE_YOUR_FACE_SUBSCRIPTION_KEY_HERE";

        final AzureRegions REGION = AzureRegions.WESTUS;

        FaceAPI client = FaceAPIManager.authenticate(REGION, KEY);
       

        System.out.println("============== Detect Face ==============");
        List<UUID> singleFaceIDs = detectFaces(client, SINGLE_FACE_URL, SINGLE_IMAGE_NAME);
        List<UUID> groupFaceIDs = detectFaces(client, GROUP_FACES_URL, GROUP_IMAGE_NAME);

        System.out.println("============== Find Similar ==============");
        findSimilar(client, singleFaceIDs, groupFaceIDs, GROUP_IMAGE_NAME);
        
        System.out.println("============== Verify ==============");
        verify(client, IMAGE_BASE_URL);

        System.out.println("============== Identify ==============");
        identifyFaces(client, IMAGE_BASE_URL, PERSON_GROUP_ID);
        
        System.out.println("============== Group Faces ==============");
        groupFaces(client, IMAGE_BASE_URL);
        
        System.out.println("============== Face Lists ==============");
        faceLists(client, IMAGE_BASE_URL, FACE_LIST_ID);

        System.out.println("============== Delete ==============");
        delete(client, PERSON_GROUP_ID, FACE_LIST_ID);
    }
 
    public static List<UUID> detectFaces(FaceAPI client, String imageURL, String imageName) {
        List<DetectedFace> facesList = client.faces().detectWithUrl(imageURL, new DetectWithUrlOptionalParameter().withReturnFaceId(true));
        System.out.println("Detected face ID(s) from URL image: " + imageName  + " :");
        List<UUID> faceUuids = new ArrayList<>();
        for (DetectedFace face : facesList) {
            faceUuids.add(face.faceId());
            System.out.println(face.faceId()); 
        }
        System.out.println();

        return faceUuids;
    }
   
    public static List<UUID> findSimilar(FaceAPI client, List<UUID> singleFaceList, List<UUID> groupFacesList, String groupImageName) {
        List<SimilarFace> listSimilars = client.faces().findSimilar(singleFaceList.get(0),
                                             new FindSimilarOptionalParameter().withFaceIds(groupFacesList));
        System.out.println();
        System.out.println("Similar faces found in group photo " + groupImageName + " are:");
        List<UUID> similarUuids = new ArrayList<>();
        for (SimilarFace face : listSimilars) {
            similarUuids.add(face.faceId());
            System.out.println("Face ID: " + face.faceId());
            System.out.println("Confidence: " + face.confidence());
        }
        System.out.println();

        return similarUuids;
    }

    public static void verify(FaceAPI client, String imageBaseURL) {

        String sourceImage1 = "Family1-Dad3.jpg";
        String sourceImage2 = "Family1-Son1.jpg";

        List<String> targetImages = new ArrayList<>();
        targetImages.add("Family1-Dad1.jpg");
        targetImages.add("Family1-Dad2.jpg");

        List<UUID> source1ID = detectFaces(client, imageBaseURL + sourceImage1, sourceImage1);
        List<UUID> source2ID = detectFaces(client, imageBaseURL + sourceImage2, sourceImage2);

        List<UUID> targetIDs = new ArrayList<>(); 

        for (String face : targetImages) {
            List<UUID> faceId = detectFaces(client, imageBaseURL + face, face);
            targetIDs.add(faceId.get(0));
        }

        VerifyResult sameResult = client.faces().verifyFaceToFace(source1ID.get(0), targetIDs.get(0));
        System.out.println(sameResult.isIdentical() ? 
            "Faces from " + sourceImage1 + " & " + targetImages.get(0) + " are of the same person." : 
            "Faces from " + sourceImage1 + " & " + targetImages.get(0) + " are different people.");

        
        VerifyResult differentResult = client.faces().verifyFaceToFace(source2ID.get(0), targetIDs.get(0));
        System.out.println(differentResult.isIdentical() ? 
            "Faces from " + sourceImage2 + " & " + targetImages.get(1) + " are of the same person." : 
            "Faces from " + sourceImage2 + " & " + targetImages.get(1) + " are different people.");

            System.out.println();
    }

    public static void identifyFaces(FaceAPI client, String imageBaseURL, String personGroupID) {
        Map<String, String[]> facesList = new HashMap<String, String[]>();
        facesList.put("Family1-Dad", new String[] { "Family1-Dad1.jpg", "Family1-Dad2.jpg" });
        facesList.put("Family1-Mom", new String[] { "Family1-Mom1.jpg", "Family1-Mom2.jpg" });
        facesList.put("Family1-Son", new String[] { "Family1-Son1.jpg", "Family1-Son2.jpg" });
        facesList.put("Family1-Daughter", new String[] { "Family1-Daughter1.jpg", "Family1-Daughter2.jpg" });
        facesList.put("Family2-Lady", new String[] { "Family2-Lady1.jpg", "Family2-Lady2.jpg" });
        facesList.put("Family2-Man", new String[] { "Family2-Man1.jpg", "Family2-Man2.jpg" });

        String groupPhoto = "identification1.jpg";

        System.out.println("Creating the person group " + personGroupID + " ...");
        client.personGroups().create(personGroupID, new CreatePersonGroupsOptionalParameter().withName(personGroupID));
        
        
        for (String personName : facesList.keySet()) {
            UUID personID = UUID.randomUUID();
            Person person = client.personGroupPersons().create(personGroupID, 
                    new CreatePersonGroupPersonsOptionalParameter().withName(personName));

            for (String personImage : facesList.get(personName)) {
                client.personGroupPersons().addPersonFaceFromUrl(personGroupID, person.personId(), imageBaseURL + personImage, null);
            } 
        }  

        System.out.println();
        System.out.println("Training person group " + personGroupID + " ...");
        client.personGroups().train(personGroupID);

        while(true) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) { e.printStackTrace(); }
            
            TrainingStatus status = client.personGroups().getTrainingStatus(personGroupID);
            if (status.status() == TrainingStatusType.SUCCEEDED) {
                System.out.println("Training status: " + status.status());
                break;
            }
            System.out.println("Training status: " + status.status());
        }
        System.out.println();

        List<UUID> detectedFaces = detectFaces(client, imageBaseURL + groupPhoto, groupPhoto);
        List<IdentifyResult> identifyResults = client.faces().identify(personGroupID, detectedFaces, null);
    
        System.out.println("Persons identified in group photo " + groupPhoto + ": ");
        for (IdentifyResult person : identifyResults) {
            System.out.println("Person ID: " + person.faceId().toString() 
                        + " with confidence " + person.candidates().get(0).confidence());
        }
    }

    public static void groupFaces(FaceAPI client, String imageBaseURL) {

        List<String> imagesList = new ArrayList<>();
        imagesList.add("Family1-Dad1.jpg");
        imagesList.add("Family1-Dad2.jpg");
        imagesList.add("Family3-Lady1.jpg");
        imagesList.add("Family1-Daughter1.jpg");
        imagesList.add("Family1-Daughter2.jpg");
        imagesList.add("Family1-Daughter3.jpg");


        Map<String, String> faces = new HashMap<>();
        List<UUID> faceIds = new ArrayList<>();


        for (String image : imagesList) {

            List<UUID> detectedFaces = detectFaces(client, imageBaseURL + image, image);
            faceIds.add(detectedFaces.get(0)); 
            faces.put(detectedFaces.get(0).toString(), image);
        }

        GroupResult results = client.faces().group(faceIds);

        for (int i = 0; i < results.groups().size(); i++) {
            System.out.println("Found face group " + (i + 1) + ": ");
            for (UUID id : results.groups().get(i)) {

                System.out.println(id);
            }
            System.out.println();
        }


        System.out.println("Found messy group: ");
        for (UUID mID : results.messyGroup()) {
            System.out.println(mID);
        }
        System.out.println();
    }

    public static void faceLists(FaceAPI client, String imageBaseURL, String faceListID) {

        List<String> imagesList = new ArrayList<>();
        imagesList.add("Family1-Dad1.jpg");
        imagesList.add("Family1-Dad2.jpg");
        imagesList.add("Family3-Lady1.jpg");
        imagesList.add("Family1-Daughter1.jpg");
        imagesList.add("Family1-Daughter2.jpg");
        imagesList.add("Family1-Daughter3.jpg");

        
        System.out.println("Creating the face list " + faceListID + " ...");
        client.faceLists().create(faceListID, new CreateFaceListsOptionalParameter().withName(faceListID));


        for (String image : imagesList) {

            client.faceLists().addFaceFromUrl(faceListID, imageBaseURL + image, null);
        }

        FaceList retrievedFaceList = client.faceLists().get(faceListID);

        System.out.println("Face list IDs: ");
        for (PersistedFace face : retrievedFaceList.persistedFaces()) {
            System.out.println(face.persistedFaceId());
        }
        System.out.println();
    }

    public static void delete(FaceAPI client, String personGroupID, String faceListID){

        System.out.println("Deleting the person group...");
        client.personGroups().delete(personGroupID);
        System.out.println("Deleted the person group " + personGroupID);


        System.out.println("Deleting the face list...");
        client.faceLists().delete(faceListID);
        System.out.println("Deleted the face list " + faceListID);
    }
  
}