package project.hnoct.kitchen.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;

public class ActivityFavorites extends AppCompatActivity implements FragmentFavorites.RecipeCallBack {

    @BindView(R.id.toolbar) Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onItemSelected(String recipeUrl, String imageUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
        // If in single-view mode, then start the ActivityRecipeDetails
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                new Pair(viewHolder.recipeImage, getString(R.string.transition_recipe_image))
        );
        Intent intent = new Intent(this, ActivityRecipeDetails.class);
        intent.setData(Uri.parse(recipeUrl));
        intent.putExtra(getString(R.string.extra_image), imageUrl);
        ActivityCompat.startActivity(this, intent, options.toBundle());
    }
}
