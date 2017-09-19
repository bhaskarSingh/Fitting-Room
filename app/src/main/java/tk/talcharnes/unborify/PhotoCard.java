
package tk.talcharnes.unborify;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuInflater;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.iarcuschin.simpleratingbar.SimpleRatingBar;
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

import tk.talcharnes.unborify.Utilities.FirebaseConstants;
import tk.talcharnes.unborify.Utilities.PhotoUtilities;

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

    @View(R.id.userImage)
    private ImageView userImageView;

    @View(R.id.realPhotoSwipeCard)
    private CardView realPhotoSwipeCard;

    @View(R.id.thumbs_up_fab)
    private FloatingActionButton likeButton;

    @View(R.id.thumbs_down_fab)
    private FloatingActionButton dislikeButton;

    @View(R.id.nameText)
    private TextView nameTextView;

    @View(R.id.ratingbar)
    private SimpleRatingBar ratingBar;

    @View(R.id.uploadederNameTxt)
    private TextView usernameTextView;

    @View(R.id.zoom_button)
    private ImageButton zoom_button;

    @View(R.id.comment_button)
    private ImageButton comment_button;

    @View(R.id.photo_card_options)
    private ImageButton photo_card_options;

    private Photo mPhoto;
    private Context mContext;
    private SwipePlaceHolderView mSwipeView;
    private String mUserId, mUserName;
    private DatabaseReference mPhotoReference, mReportsRef;
    private Boolean isReported = false;
    static boolean isAd = false;
    private int width;
    private int height;
    private boolean mVisible = true;

    public PhotoCard(Context context, Photo photo, SwipePlaceHolderView swipeView, String userId,
                     String userName, DatabaseReference photoReference,
                     DatabaseReference reportsRef) {
        mContext = context;
        mPhoto = photo;
        mSwipeView = swipeView;
        mUserId = userId;
        mUserName = userName;
        mPhotoReference = photoReference;
        mReportsRef = reportsRef;
        isAd = photo.isAd();
        Log.d(LOG_TAG, "isAd = " + isAd);

    }

    /**
     * This function sets up the Card View with an image, name, and the ratings.
     */
    @Resolve
    private void onResolved() {
        final String url = mPhoto.getUrl();
        if (url != null && !url.isEmpty()) {
            final int rotation = getRotation();
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child(FirebaseConstants.IMAGES).child(url);

            Glide.with(mContext)
                    .using(new FirebaseImageLoader())
                    .load(storageRef).transform(new MyTransformation(mContext, rotation))
                    .into(photoImageView);
            float dislikes = Float.parseFloat("" + mPhoto.getDislikes());
            nameTextView.setText(mPhoto.getOccasion_subtitle());
            float likes = Float.parseFloat("" + mPhoto.getLikes());
            dislikes = (dislikes < 1) ? 1 : dislikes;
            likes = (likes < 1) ? 1 : likes;
            float totalVotes = likes + dislikes;
            float rating = (likes / totalVotes) * 100f;
            System.out.println("Likes: " + likes + "---------------- Dislikes: "
                    + dislikes + "----------- TotalVotes: " + totalVotes + "----------- Rating: " +
                    rating);
            int index = (int) Math.floor(rating/20f);
            int[] ratingColors = mContext.getResources().getIntArray(R.array.array_rate_colors);
            int[] ratingShadowColors = mContext.getResources().getIntArray(R.array.array_rate_shadow_colors);

            ratingBar.setFillColor(ratingColors[index]);
            ratingBar.setBorderColor(ratingShadowColors[index]);
            ratingBar.setRating(rating/20f);

            ImageButton x = realPhotoSwipeCard.findViewById(R.id.zoom_button);
            zoom_button.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    Intent intent = new Intent(mContext, ZoomPhoto.class);
                    intent.putExtra("url", mPhoto.getUrl());
                    intent.putExtra("rotation", rotation);
                    mContext.startActivity(intent);
                }
            });

            comment_button.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    Intent intent = new Intent(mContext, CommentActivity.class);
                    intent.putExtra("url", mPhoto.getUrl());
                    intent.putExtra("photoUserID", mPhoto.getUser());
                    intent.putExtra("currentUser", mUserId);
                    intent.putExtra("name", mUserName);
                    mContext.startActivity(intent);
                }
            });

            photo_card_options.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    showPopup(view, 0);
                }
            });

            setUploaderName(mPhoto, usernameTextView);

            Glide.with(mContext).load("http://www.womenshealthmag.com/sites/womenshealthmag.com/files/images/power-of-smile_0.jpg")
                    .crossFade()
                    .thumbnail(.5f)
                    .bitmapTransform(new CircleTransform(mContext))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(userImageView);

            likeButton.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    Analytics.registerSwipe(mContext, "right");
                    mSwipeView.doSwipe(true);
                }
            });

            dislikeButton.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    Analytics.registerSwipe(mContext, "left");
                    mSwipeView.doSwipe(false);
                }
            });
        }
    }

    /**
     * This function handles when the Card View is clicked.
     */
    @Click(R.id.photoImageView)
    private void onClick() {
        Log.d("EVENT", "profileImageView click");
        //mSwipeView.addView(this);
        togglePhotoAddOns();

    }

    /**
     * This function handles when the Card View is swiped right.
     */
    @SwipeIn
    private void onSwipeIn() {
        //Log.d("EVENT", "onSwipedIn");
        final boolean itsAnAd = isAd;
        if (!itsAnAd) {
            setVote("likes");
            Analytics.registerSwipe(mContext, "right");
        }
    }

    /**
     * This function handles when the Card View is swiped left.
     */
    @SwipeOut
    private void onSwipedOut() {
        //Log.d("EVENT", "onSwipedOut");
        final boolean itsAnAd = isAd;
        if (!itsAnAd) {
            if (isReported != null && isReported) {
                isReported = false;
                setReport();
            } else {
                setVote("dislikes");
                Analytics.registerSwipe(mContext, "left");
            }
        }
    }

    /**
     * This function handles when the Card View is moving right.
     */
    @SwipeInState
    private void onSwipeInState() {
        //Log.d("EVENT", "onSwipeInState");
    }

    /**
     * This function handles when the Card View is moving left.
     */
    @SwipeOutState
    private void onSwipeOutState() {
        //Log.d("EVENT", "onSwipeOutState");
    }

    /**
     * Don't know what this does.
     */
    @SwipeCancelState
    private void onSwipeCancelState() {
        Log.d("EVENT", "onSwipeCancelState");
    }

    /**
     * This function records the direction of user touches.
     */
    @SwipingDirection
    private void onSwipingDirection(SwipeDirection direction) {
        Log.d(LOG_TAG, "SwipingDirection " + direction.name());
    }

    /**
     * This function records the user's vote in the database.
     */
    private void setVote(final String rating) {
        final boolean itsAnAd = isAd;
        if (!itsAnAd) {
            final String userID = mUserId;
            final String name = PhotoUtilities.removeWebPFromUrl(mPhoto.getUrl());
            final DatabaseReference chosenPhoto = mPhotoReference.child(name);
            if (!mUserId.equals(mPhoto.getUser())) {
                chosenPhoto.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child(FirebaseConstants.VOTES).child(userID).exists()) {
                            String uRating = (dataSnapshot.child(FirebaseConstants.VOTES).child(userID).getValue() + "");
                            if (uRating.equals(rating)) {
                                Log.d(LOG_TAG, "The User already " + rating + " the photo.");
                            } else {
                                String rating2 = (rating.equals("likes")) ? "dislikes" : "likes";
                                long ratingValue = (long) dataSnapshot.child(rating).getValue();
                                long ratingValue2 = (long) dataSnapshot.child(rating2).getValue();
                                chosenPhoto.child(rating).setValue(ratingValue + 1);
                                chosenPhoto.child(rating2).setValue(ratingValue2 - 1);
                                chosenPhoto.child(FirebaseConstants.VOTES).child(userID).setValue(rating);
                            }
                        } else {
                            final long ratingValue = (long) dataSnapshot.child(rating).getValue();
                            chosenPhoto.child(rating).setValue(ratingValue + 1);
                            chosenPhoto.child(FirebaseConstants.VOTES).child(userID).setValue(rating);
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
    }

    /**
     * This function changes the value of isReported.
     */
    public void setReported() {
        final boolean itsAnAd = isAd;
        if (!itsAnAd) {
            isReported = true;
        }
    }

    /**
     * This function records the user's report in the database.
     */
    private void setReport() {
        final boolean itsAnAd = isAd;
        if (!itsAnAd) {
            final String userID = mUserId;
            final String name = PhotoUtilities.removeWebPFromUrl(mPhoto.getUrl());
            final Query query = mReportsRef.child(name);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        if (snapshot.child(FirebaseConstants.REPORTED_BY).child(userID).exists()) {
                            Log.d(LOG_TAG, "User already reported photo.");
                        } else {
                            long numReports = (long) snapshot.child(FirebaseConstants.NUM_REPORTS).getValue();
                            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US).format(new Date());
                            mPhotoReference.child(name).child(FirebaseConstants.PHOTO_REPORTS).setValue(numReports + 1);
                            mReportsRef.child(name).child(FirebaseConstants.NUM_REPORTS).setValue(numReports + 1);
                            mReportsRef.child(name).child(FirebaseConstants.REPORTED_BY).child(userID).setValue(timeStamp);
                            Log.d(LOG_TAG, "Another report add.");
                        }
                    } else {
                        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US).format(new Date());
                        HashMap<String, String> reports = new HashMap<String, String>();
                        reports.put(userID, timeStamp);
                        Report report = new Report(1, reports);
                        mReportsRef.child(name).setValue(report);
                        mPhotoReference.child(name).child(FirebaseConstants.PHOTO_REPORTS).setValue(1);
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

    public void setUploaderName(Photo photo, final TextView usernameTextView) {
        String userName = photo.getUserName();
        if (userName != null && !userName.isEmpty() && !userName.equals("")) {
            String name = "By: " + userName;
            usernameTextView.setText(name);
        }
        else usernameTextView.setText("By: Anonymous User");
    }


    private int pxToDp(int px) {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    private int getRotation() {
        int rotation = 0;
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (mPhoto.getOrientation() != 0) {
                rotation = 90;
            }
        } else if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (mPhoto.getOrientation() == 0) {
                rotation = 90;
            }
        }
        return rotation;
    }

    private void togglePhotoAddOns() {
        if (mVisible) {
            zoom_button.setVisibility(android.view.View.GONE);
            comment_button.setVisibility(android.view.View.GONE);
            photo_card_options.setVisibility(android.view.View.GONE);
            mVisible = false;
        } else {
            zoom_button.setVisibility(android.view.View.VISIBLE);
            comment_button.setVisibility(android.view.View.VISIBLE);
            photo_card_options.setVisibility(android.view.View.VISIBLE);

            mVisible = true;
        }
    }

    private void showPopup(android.view.View v, final int i) {
        PopupMenu popup = new PopupMenu(mContext, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_photo_card, popup.getMenu());
        popup.show();
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(
                    android.view.MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_report_photo:
                        setReported();
                        mSwipeView.doSwipe(false);
                        return true;
                    default:
                        return false;
                }
                //return false;
            }
        });
    }

}
