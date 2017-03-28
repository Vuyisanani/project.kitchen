package project.hnoct.kitchen.ui;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.dialog.RecipeBookDetailsDialog;

public class RecipeBookActivity extends AppCompatActivity implements RecipeBookDetailsDialog.RecipeBookDetailsListener {
    /** Constants **/
    private static final String RECIPE_BOOK_DETAILS_DIALOG = "recipe_book_details_dialog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_book);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Shows the dialog for creating/editing the details of the recipe book
     */
    void showRecipeBookDetailDialog() {
        RecipeBookDetailsDialog dialog = new RecipeBookDetailsDialog();
        dialog.show(getFragmentManager(), RECIPE_BOOK_DETAILS_DIALOG);
    }

    // TODO: Finish this stub
    @Override
    public void onPositiveDialogClick(DialogFragment dialog, String titleText, String descriptionText) {

    }
}