package project.hnoct.kitchen.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.FileNotFoundException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.Utilities;

/**
 * A placeholder fragment containing a simple view.
 */
public class CreateRecipeActivityFragment extends Fragment {
    /** Constants **/
    private static final String LOG_TAG = CreateRecipeActivity.class.getSimpleName();
    private final int SELECT_PHOTO = 25687;

    /** Member Variables **/
    Context mContext;
    long mRecipeId;
    String mRecipeImageUri;
    String mRecipeDescription;
    String mRecipeName;
    boolean mSaved = false;     // Check if the user has saved the data manually

    Bitmap mImageBitmap;

    // ButterKnife Binding
    @BindView(R.id.create_recipe_name_edit_text) EditText mRecipeNameEditText;
    @BindView(R.id.create_recipe_description_edit_text) EditText mRecipeDescriptionEditText;
    @BindView(R.id.create_recipe_image) ImageView mRecipeImage;
    @BindView(R.id.create_recipe_ingredient_recycler_view) RecyclerView mIngredientRecyclerView;

    @OnClick(R.id.create_recipe_image)
    public void selectImage() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }

    public CreateRecipeActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_create_recipe, container, false);
        ButterKnife.bind(this, rootView);

        // Initialize member variables
        mContext = getActivity();

        // Attempt to retrieve saved data
        getAutosavedData();
        Log.d(LOG_TAG, "RecipeId: " + mRecipeId);
        Log.d(LOG_TAG, "Recipe name: " + mRecipeName);
        Log.d(LOG_TAG, "Recipe description: " + mRecipeDescription);

        if (mRecipeId == 0) {
            // If no saved data exists, generate a new recipeId
            mRecipeId = Utilities.generateNewId(mContext, 10000, Utilities.RECIPE_TYPE);
        } else {
            // Insert saved data into EditText
            mRecipeNameEditText.setText(mRecipeName, TextView.BufferType.EDITABLE);
            mRecipeDescriptionEditText.setText(mRecipeDescription, TextView.BufferType.EDITABLE);
            Glide.with(mContext).load(mRecipeImageUri).into(mRecipeImage);

            // Delete the autosaved data so it will not show up again after they've completed this
            // recipe
            deleteAutosavedData();
        }

        // Instantiate the RecyclerAdapters
        AddIngredientAdapter ingredientAdapter = new AddIngredientAdapter(mContext);

        // Instantiate the LinearLayoutManagers
        LinearLayoutManager llm = new LinearLayoutManager(mContext);

        // Set the LLM and Adapters to the RecyclerViews
        mIngredientRecyclerView.setLayoutManager(llm);
        mIngredientRecyclerView.setAdapter(ingredientAdapter);

        return rootView;
    }

    @Override
    public void onPause() {
        if (!mSaved) {
            autosaveUserInput();
        }
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check to make sure that data was received correctly and has the correct request code
        if (requestCode == SELECT_PHOTO && resultCode == Activity.RESULT_OK && data != null) {
            // Get the URI of the image selected
            Uri selectedImageUri = data.getData();

            try {
                // Create a bitmap by turning the bitmap into an InputStream
                InputStream inputStream = mContext.getContentResolver().openInputStream(selectedImageUri);

                // Decode the bitmap from the stream
                mImageBitmap = BitmapFactory.decodeStream(inputStream);

                // Save the bitmap to file in the private directory
                /** @see Utilities#saveImageToFile(Context, long, Bitmap) **/
                mRecipeImageUri = Utilities.saveImageToFile(mContext, mRecipeId, mImageBitmap);

                // Load the image into the ImageView
                Glide.with(mContext).load(mRecipeImageUri).into(mRecipeImage);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieves saved data from SharedPreferences from when user last left this Activity
     */
    private void getAutosavedData() {
        // Initialize SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Get the stored data
        mRecipeName = prefs.getString(mContext.getString(R.string.edit_recipe_name_key), null);
        mRecipeDescription = prefs.getString(mContext.getString(R.string.edit_recipe_description_key), null);
        mRecipeImageUri = prefs.getString(mContext.getString(R.string.edit_recipe_image_key), null);

        if (mRecipeName != null || mRecipeDescription != null || mRecipeImageUri != null) {
            // If at least one piece of data was retrieved, then get the recipeId
            mRecipeId = prefs.getLong(mContext.getString(R.string.edit_recipe_id_key), 0);
        }
    }

    /**
     * Saves user input to SharedPreferences in case application is accidentally exited or the
     * user accidentally leaves the activity
     */
    private void autosaveUserInput() {
        // Save the user's input when leaving the activity so they can pick up where they left off
        // Get the String entered by the user into the EditText Views
        mRecipeName = mRecipeNameEditText.getText().toString();
        mRecipeDescription = mRecipeDescriptionEditText.getText().toString();

        // Initialize SharedPreferences and its Editor
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();

        // Save data to SharedPreferences if it exists
        if (mRecipeName != null) {
            editor.putString(
                    mContext.getString(R.string.edit_recipe_name_key),
                    mRecipeName
            );
        }
        if (mRecipeDescription != null) {
            editor.putString(
                    mContext.getString(R.string.edit_recipe_description_key),
                    mRecipeDescription
            );
        }
        if (mRecipeImageUri != null) {
            editor.putString(
                    mContext.getString(R.string.edit_recipe_image_key),
                    mRecipeImageUri
            );
        }
        if (mRecipeName != null || mRecipeDescription != null || mRecipeImage != null) {
            // RecipeId only needs to be saved if other data was saved as well.
            editor.putLong(
                    mContext.getString(R.string.edit_recipe_id_key),
                    mRecipeId
            );
        }
        // Apply the changes
        editor.apply();

        Toast.makeText(mContext, "Data saved!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Wipes data in SharedPreferences after it has been loaded into memory
     */
    private void deleteAutosavedData() {
        // Instantiate SharedPreferences and its Editor
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();

        // Place null values in the SharedPreferences
        editor.putString(
                mContext.getString(R.string.edit_recipe_name_key),
                null
        );
        editor.putString(
                mContext.getString(R.string.edit_recipe_description_key),
                null
        );
        editor.putString(
                mContext.getString(R.string.edit_recipe_image_key),
                null
        );
        editor.putLong(
                mContext.getString(R.string.edit_recipe_id_key),
                0
        );
    }
}
