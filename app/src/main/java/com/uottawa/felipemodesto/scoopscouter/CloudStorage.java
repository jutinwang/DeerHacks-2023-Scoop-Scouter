package com.uottawa.felipemodesto.scoopscouter;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class CloudStorage extends AppCompatActivity {
    public void AddData(Timestamp t, GeoPoint gp) {
        // Access a Cloud Firestore instance from your Activity
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference maxIdRef = db.collection("indices").document("max_truck_id");
        final String TAG = "DocSnippets";
        maxIdRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    int id = 1;
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        // get id # from max_truck_id and assign it to this name and increment it
                        id = Integer.parseInt(document.get("id").toString());
                        Log.d(TAG, "Max ID: " + document.get("id").toString());
                        Log.d(TAG, "New ID: " + id);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                    Map<String, Object> truckEntry = new HashMap<>();
                    String fileName = "truck_" + Integer.toString(id);
                    DocumentReference newTruckRef = db.collection("ice_cream_trucks").document(fileName);
                    truckEntry.put("location", gp);
                    String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(gp.getLatitude(), gp.getLongitude()), 22);
                    truckEntry.put("geohash", hash);
                    truckEntry.put("timestamp", t);
                    newTruckRef.set(truckEntry);
                    maxIdRef.update("id", FieldValue.increment(1));
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    public void GetNearbyTrucks(GeoPoint curGP, int zoomLevel) {
        // TODO: make up a formula
        final String TAG = "DocSnippets";
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference trucksRef = db.collection("ice_cream_trucks");
        //Query nearbyQuery = trucksRef.whereEqualTo("")
//        trucksRef
//                .whereGreaterThan("lat", curGP.getLatitude() - 5)
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                Log.d(TAG, document.getId() + " => " + document.getData());
//                            }
//                        } else {
//                            Log.d(TAG, "Error getting documents: ", task.getException());
//                        }
//                    }
//                });
        // Find cities within 50km of London
        final GeoLocation center = new GeoLocation(curGP.getLatitude(), curGP.getLongitude());
        double zoomMultiplier = 1 / zoomLevel * 50;
        final double radiusInM = 1000; // CONSTANT FOR NOW

        // Each item in 'bounds' represents a startAt/endAt pair. We have to issue
        // a separate query for each pair. There can be up to 9 pairs of bounds
        // depending on overlap, but in most cases there are 4.
        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (GeoQueryBounds b : bounds) {
            Query q = db.collection("cities")
                    .orderBy("geohash")
                    .startAt(b.startHash)
                    .endAt(b.endHash);

            tasks.add(q.get());
        }
        List<GeoPoint> markerPoints = new ArrayList<>();
        // Collect all the query results together into a single list
        Tasks.whenAllComplete(tasks)
                .addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Task<?>>> t) {
                        List<DocumentSnapshot> matchingDocs = new ArrayList<>();

                        for (Task<QuerySnapshot> task : tasks) {
                            QuerySnapshot snap = task.getResult();
                            for (DocumentSnapshot doc : snap.getDocuments()) {
//                                double lat = doc.getDouble("lat");
//                                double lng = doc.getDouble("lng");
                                GeoPoint gp = doc.getGeoPoint("location");

                                // We have to filter out a few false positives due to GeoHash
                                // accuracy, but most will match
                                // GeoLocation docLocation = new GeoLocation(lat, lng);
                                GeoLocation docLocation = new GeoLocation(gp.getLatitude(), gp.getLongitude());
                                double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                                if (distanceInM <= radiusInM) {
                                    matchingDocs.add(doc);
                                    markerPoints.add(new GeoPoint(gp.getLatitude(), gp.getLongitude()));
                                }
                            }
                        }
                        // matchingDocs contains the results
                        // ...
                        ////// CALL DISPLAY ALL MARKERS ////// ( can adapt to accept List<DocumentSnapshot>)
                        MainActivity.getInstance().placeMarkers(markerPoints);
                    }
                });

    }

    public void GetImage(int id, int imgNum) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        // Create a storage reference from this app
        StorageReference storageRef = storage.getReference();
        // Creates a child reference to "scoop_scouter"
        StorageReference imagesRef = storageRef.child("ice-cream-truck-images/");
        String imgName = "truck" + id;

        StorageReference truckImgRef = imagesRef.child(imgName);
        final long ONE_MEGABYTE = 1024 * 1024;
        truckImgRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Data for "images/island.jpg" is returns, use this as needed
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure (@NonNull Exception exception) {
                // Handle any errors
            }
        });

    }

    public void GetImage(int id) {
        GetImage(id, 0);
    }
}
