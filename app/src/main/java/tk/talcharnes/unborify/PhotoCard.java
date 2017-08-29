
package tk.talcharnes.unborify;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mindorks.placeholderview.SwipeDirection;
import com.mindorks.placeholderview.SwipePlaceHolderView;
import com.mindorks.placeholderview.annotations.Click;
import com.mindorks.placeholderview.annotations.Layout;
import com.mindorks.placeholderview.annotations.NonReusable;
import com.mindorks.placeholderview.annotations.Resolve;
import com.mindorks.placeholderview.annotations.View;
import com.mindorks.placeholderview.annotations.swipe.SwipeCancelState;
import com.mindorks.placeholderview.annotations.swipe.SwipeIn;
import com.mindorks.placeholderview.annotations.swipe.SwipeInState;
import com.mindorks.placeholderview.annotations.swipe.SwipeOut;
import com.mindorks.placeholderview.annotations.swipe.SwipeOutState;
import com.mindorks.placeholderview.annotations.swipe.SwipingDirection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by janisharali on 19/08/16.
 * Modified by Khuram Chaudhry on 08/28/2017.
 */
@NonReusable
@Layout(R.layout.photo_card_view)
public class PhotoCard {

    private final String LOG_TAG = PhotoCard.class.getSimpleName();

    @View(R.id.photoImageView)
    private ImageView photoImageView;

    @View(R.id.likesText)
    private TextView likeTextView;

    @View(R.id.nameText)
    private TextView nameTextView;

    @View(R.id.dislikesText)
    private TextView dislikeTextView;

    private Photo mPhoto;
    private Context mContext;
    private SwipePlaceHolderView mSwipeView;
    private String mUserId;
    private DatabaseReference mPhotoReference, mReportsRef;
    static Boolean isReported = false;

    public PhotoCard(Context context, Photo photo, SwipePlaceHolderView swipeView, String userId,
                     DatabaseReference photoReference, DatabaseReference reportsRef) {
        mContext = context;
        mPhoto = photo;
        mSwipeView = swipeView;
        mUserId = userId;
        mPhotoReference = photoReference;
        mReportsRef = reportsRef;
    }

    @Resolve
    private void onResolved(){
        String url = mPhoto.getUrl();
        if(url != null && !url.isEmpty()) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images").child(url);
            Glide.with(mContext)
                    .using(new FirebaseImageLoader())
                    .load(storageRef)
                    .into(photoImageView);
            String dislikes = "Dislikes: "+mPhoto.getDislikes();
            dislikeTextView.setText(dislikes);
            nameTextView.setText(mPhoto.getOccasion_subtitle());
            String likes = "Likes: "+mPhoto.getLikes();
            likeTextView.setText(likes);
        }
    }

    @Click(R.id.photoImageView)
    private void onClick(){
        Log.d("EVENT", "profileImageView click");
        //mSwipeView.addView(this);
    }

    @SwipeIn
    private void onSwipeIn(){
        //Log.d("EVENT", "onSwipedIn");
        setVote("likes");
    }

    @SwipeOut
    private void onSwipedOut(){
        //Log.d("EVENT", "onSwipedOut");
        if(isReported) {
            isReported = false;
            setReport();
        } else {
            setVote("dislikes");
        }
    }

    @SwipeInState
    private void onSwipeInState(){
        //Log.d("EVENT", "onSwipeInState");
    }

    @SwipeOutState
    private void onSwipeOutState(){
        //Log.d("EVENT", "onSwipeOutState");
    }

    @SwipeCancelState
    private void onSwipeCancelState(){
        Log.d("EVENT", "onSwipeCancelState");
    }

    @SwipingDirection
    private void onSwipingDirection(SwipeDirection direction) {
        Log.d(LOG_TAG, "SwipingDirection " + direction.name());
    }

    /**
     * This function records the user's vote in the database.
     * */
    private void setVote(final String rating) {
        final String userID = mUserId;
        Log.d(LOG_TAG, "<------------------"+userID+"---------------------->");
        if (!mUserId.equals(mPhoto.getUser())) {
            final String name = mPhoto.getUrl().replace(".webp", "");
            mPhotoReference.child(name).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child("votes").child(userID).exists()) {
                        String uRating = (dataSnapshot.child("votes").child(userID).getValue()+"");
                        if(uRating.equals(rating)) {
                            Log.d(LOG_TAG, "The already User " + rating + " the photo.");
                        } else {
                            String rating2 = (rating.equals("likes")) ? "dislikes" : "likes";
                            long ratingValue = (long) dataSnapshot.child(rating).getValue();
                            long ratingValue2 = (long) dataSnapshot.child(rating2).getValue();
                            mPhotoReference.child(name).child(rating).setValue(ratingValue + 1);
                            mPhotoReference.child(name).child(rating2).setValue(ratingValue2 - 1);
                            mPhotoReference.child(name).child("votes").child(userID).setValue(rating);
                        }
                    } else {
                        long ratingValue = (long) dataSnapshot.child(rating).getValue();
                        mPhotoReference.child(name).child(rating).setValue(ratingValue + 1);
                        mPhotoReference.child(name).child("votes").child(userID).setValue(rating);
                        Log.d(LOG_TAG, "The User " + rating + " the photo.");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(LOG_TAG, "cancelled with error - " + databaseError);
                }
            });
        } else {
            Log.d(LOG_TAG, "User trying to vote on own photo");
        }
    }

    /**
     * This function changes the value of isReported.
     * */
    public static void setReported() {
        isReported = true;
    }

    /**
     * This function records the user's report in the database.
     * */
    private void setReport() {
        final String name = mPhoto.getUrl().replace(".webp","");
        System.out.println("------------------------------"+name+"----------------------------------");
        final Query query = mReportsRef.child(name);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    if(snapshot.child("reported_by").child(mUserId).exists()) {
                        Log.d(LOG_TAG, "User already reported photo.");
                    } else {
                        long numReports = (long) snapshot.child("numReports").getValue();
                        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US).format(new Date());
                        mPhotoReference.child(name).child("reports").setValue(numReports + 1);
                        mReportsRef.child(name).child("numReports").setValue(numReports + 1);
                        mReportsRef.child(name).child("reported_by").child(mUserId).setValue(timeStamp);
                        Log.d(LOG_TAG, "Another report add.");
                    }
                } else {
                    String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US).format(new Date());
                    HashMap<String, String> reports = new HashMap<String, String>();
                    reports.put(mUserId, timeStamp);
                    Report report = new Report(1, reports);
                    mReportsRef.child(name).setValue(report);
                    mPhotoReference.child(name).child("reports").setValue(1);
                    Log.d(LOG_TAG, "A new report.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(LOG_TAG, "cancelled with error - " + databaseError);
            }
        });
    }
}