package project.kitchen.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import butterknife.Optional;
import project.kitchen.R;

import project.kitchen.data.RecipeContract.*;
import project.kitchen.data.Utilities;
import project.kitchen.ui.FragmentRecipeDetails;
import project.kitchen.ui.FragmentRecipeList;

import static project.kitchen.ui.FragmentRecipeDetails.BundleKeys.RECIPE_DETAILS_IMAGE_URL;
import static project.kitchen.ui.FragmentRecipeDetails.BundleKeys.RECIPE_DETAILS_URL;

/**
 * Created by Alvin on 2/20/2017.
 */

public class AdapterRecipe extends android.support.v7.widget.RecyclerView.Adapter<AdapterRecipe.RecipeViewHolder> {
    /** Constants **/
    public static final String LOG_TAG = AdapterRecipe.class.getSimpleName();
    private static final int RECIPE_VIEW_NORMAL = 0;
    private static final int RECIPE_VIEW_LAST = 1;
    public static final int RECIPE_VIEW_DETAIL = 2;
    public static final int RECIPE_VIEW_DETAIL_LAST = 3;
    private static final int RECIPE_VIEW_COMPACT = 4;
    private static final int RECIPE_VIEW_COMPACT_LAST = 5;

    /** Member Variables **/
    private Context mContext;
    private Cursor mCursor;
    private List<Map<String, Object>> mList;
    private RecipeAdapterOnClickHandler mClickHandler;
    private int mDetailCardPosition;
    private int mPosition;
    private int mSearchLists = 0;

    private FragmentManager mFragmentManager;       // Used for inflating the FragmentRecipeDetails into the ViewHolder
    private RecyclerView mRecyclerView;

    private boolean useDetailView = false;
    private boolean editMode = false;
    private boolean detailLoaded = false;
    private boolean favoriteView = false;

    private DetailVisibilityListener mVisibilityListener;
    private OnStartDragListener mDragListener;

    // Actions
    static final String ACTION_OPEN_DETAILS = "openDetails";
    static final String ACTION_FAVORITE = "favorite";
    static final String ACTION_UNFAVORITE = "unfavorite";

    static final String test = "test";

    public int[] editInstructions = new int[] {-1, -1, -1};

    /**
     * Public constructor for AdapterRecipe. Used to inflate view used for Recycle View in the
     * {@link FragmentRecipeList}
     * @param context interface for global context
     * @param clickHandler interface for passing information to the UI from the Adapter
     */
    public AdapterRecipe(Context context, RecipeAdapterOnClickHandler clickHandler) {
        mContext = context;
        mClickHandler = clickHandler;
        mDetailCardPosition = -1;
    }

    public void inFavoriteView() {
        favoriteView = true;
    }

    public Cursor swapCursor(Cursor newCursor) {
        boolean firstLoad = false;
        if (mCursor == null) {
            firstLoad = true;
        }

        mCursor = newCursor;
        mList = new ArrayList<>();

        if (mCursor != null) {
            if (mCursor.moveToFirst()) {

                do {
                    long recipeId = mCursor.getLong(RecipeEntry.IDX_RECIPE_ID);
                    String recipeSourceId = mCursor.getString(RecipeEntry.IDX_RECIPE_SOURCE_ID);
                    String recipeTitle = mCursor.getString(RecipeEntry.IDX_RECIPE_NAME);
                    String recipeAuthor = mCursor.getString(RecipeEntry.IDX_RECIPE_AUTHOR);
                    String recipeAttribution = mCursor.getString(RecipeEntry.IDX_RECIPE_SOURCE);
                    String recipeDescription = mCursor.getString(RecipeEntry.IDX_SHORT_DESCRIPTION);
                    String recipeImgUrl = mCursor.getString(RecipeEntry.IDX_IMG_URL);
                    int recipeReviews = mCursor.getInt(RecipeEntry.IDX_RECIPE_REVIEWS);
                    Double recipeRating = mCursor.getDouble(RecipeEntry.IDX_RECIPE_RATING);
                    boolean favorite = mCursor.getInt(RecipeEntry.IDX_FAVORITE) == 1;
                    String recipeUrl = mCursor.getString(RecipeEntry.IDX_RECIPE_URL);

                    Map<String, Object> map = new HashMap<>();

                    map.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);
                    map.put(RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
                    map.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeTitle);
                    map.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, recipeAuthor);
                    map.put(RecipeEntry.COLUMN_SOURCE, recipeAttribution);
                    map.put(RecipeEntry.COLUMN_SHORT_DESC, recipeDescription);
                    map.put(RecipeEntry.COLUMN_IMG_URL, recipeImgUrl);
                    map.put(RecipeEntry.COLUMN_REVIEWS, recipeReviews);
                    map.put(RecipeEntry.COLUMN_RATING, recipeRating);
                    map.put(RecipeEntry.COLUMN_FAVORITE, favorite);
                    map.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);

                    mList.add(map);
                } while (mCursor.moveToNext());
            }
        }

        if (firstLoad) {
            notifyItemRangeInserted(0, mList.size());
        } else {
            notifyDataSetChanged();
        }

        return mCursor;
    }

    /**
     * Modifies the list being used to populate the ViewHolders by adding additional items to mList
     * @param list List of recipes to be added
     * @return mList after the addition of the input list
     */
    public List<Map<String, Object>> addList(List<Map<String, Object>> list) {
        if (mList == null) {
            // Set mList as the input List if it has not been initialized yet
            mList = list;
        } else if (mSearchLists == 0) {
            // If it's the first item, then set mList to the input List
            mList = list;
        } else {
            // Initialize a temporary List that will hold all the recipes
            List<Map<String, Object>> tempList = new ArrayList<>();

            // Add to find the size of the combined List
            int size = mList.size() + list.size();

            // Iterate through the Lists and add recipes in in shuffled order so that the sources
            // are shuffled
            while (tempList.size() < size) {
                if (mList.size() > 0) {
                    // Add recipes from mList based on how many lists have already been added to
                    // mList
                    for (int i = 0; i < mSearchLists; i++) {
                        if (mList.size() > 0) {
                            tempList.add(mList.get(0));
                            mList.remove(0);
                        }
                    }
                }

                // Add a single item from the new List
                if (list.size() > 0) {
                    tempList.add(list.get(0));
                    list.remove(0);
                }
            }
            // Set the tempList with all recipes as the new mList;
            mList = tempList;
        }

        // Increment mSearchLists to keep track of how many Lists have already been added
        mSearchLists++;

        // Notify the Adapter of the change
        notifyDataSetChanged();

        // Return mList
        return mList;
    }

    /**
     * Retrieves the list used to store the ViewHolder's values
     * @return List of Maps with key-recipe value pairs used to populate the ViewHolder
     */
    public List<Map<String, Object>> getList() {
        return mList;
    }

    /**
     * Sets the position of this Adapter within another Adapter
     * @param position Int position of this AdapterRecipe
     */
    public void setPosition(int position) {
        mPosition = position;
    }

    /**
     * Sets whether this Adapter should utilize the DetailView Fragment when a recipe is selected
     * @param useDetailView Boolean value for whether the DetailView Fragment should be used
     * @param fragmentManager FragmentManager used to inflate the DetailView if being used
     */
    public void setUseDetailView(boolean useDetailView, FragmentManager fragmentManager) {
        this.useDetailView = useDetailView;
        mFragmentManager = fragmentManager;
    }

    /**
     * Enters "edit mode" in which the layout used for the ViewHolder is condensed so that more
     * items can be viewed and therefore be more easily re-arranged and removed
     */
    public void enterEditMode() {
        // Set editMode to true
        editMode = true;

        // Force all ViewHolder's to re-draw, binding to the new layout
        notifyDataSetChanged();
    }

    /**
     * Exits "edit mode" so that recipes are viewed with more detail
     */
    public void exitEditMode() {
        // Set editMode to false
        editMode = false;

        // Force all ViewHolder's to re-draw binding to the more detailed layout
        notifyDataSetChanged();
    }

    /**
     * Used to tell whether the Adapter is currently in edit mode
     * @return true if in edit mode, false if not in edit mode
     */
    public boolean isInEditMode() {
        return editMode;
    }

    @Override
    public RecipeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent instanceof RecyclerView) {
            if (mRecyclerView == null) {
                mRecyclerView = (RecyclerView) parent;
                hideDetails();
            }

            // Set up the view to be added to the ViewHolder
            View view;

            // Inflate the list item layout
            switch (viewType) {
                case RECIPE_VIEW_NORMAL: {
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_recipe, parent, false);
                    view.setFocusable(true);
                    break;
                }

                case RECIPE_VIEW_LAST: {
                    // Same as normal, but with an added margin at the bottom
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_recipe, parent, false);
                    view.setFocusable(true);
                    StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
                    int margin = (int) Utilities.convertDpToPixels(16);
                    params.setMargins(margin, margin, margin, margin);
                    view.setLayoutParams(params);
                    break;
                }

                case RECIPE_VIEW_DETAIL: {
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_recipe_details, parent, false);
                    view.setFocusable(true);
                    break;
                }

                case RECIPE_VIEW_DETAIL_LAST: {
                    // Same as detail, but with an added margin at the bottom
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_recipe_details, parent, false);
                    view.setFocusable(true);
                    StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
                    int margin = (int) Utilities.convertDpToPixels(16);
                    params.setMargins(margin, margin, margin, margin);
                    view.setLayoutParams(params);
                    break;
                }

                case RECIPE_VIEW_COMPACT: {
                    // For use when in edit mode
                    view = LayoutInflater.from(mContext).inflate(R.layout.list_item_recipe_compact, parent, false);
                    view.setFocusable(false);
                    break;
                }

                case RECIPE_VIEW_COMPACT_LAST: {
                    // Same as edit, but with an added margin at the bottom
                    view = LayoutInflater.from(mContext).inflate(R.layout.list_item_recipe_compact, parent, false);
                    view.setFocusable(false);
                    StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();

                    int verticalMargin = (int) Utilities.convertDpToPixels(8);
                    int horizontalMargin = (int) Utilities.convertDpToPixels(16);

                    params.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
                    view.setLayoutParams(params);
                    break;
                }

                default: throw new UnsupportedOperationException("Unknown ViewType");
            }

            // Set as the view in ViewHolder and return
            try {
                view.setTag(mPosition);
                return new RecipeViewHolder(view);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            throw new RuntimeException("Not bound to RecyclerViewSelection");
        }
    }

    /**
     * Hides the detailed layout if user scrolls it out of position
     */
    public void hideDetails() {
        // Get the layout manager
        final StaggeredGridLayoutManager sglm = (StaggeredGridLayoutManager) mRecyclerView.getLayoutManager();

        // Add an OnScrollListener
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Get the position of the first and last visible views
                int[] firstVisible = sglm.findFirstVisibleItemPositions(null);
                int[] lastVisible = sglm.findLastVisibleItemPositions(null);

                if (mDetailCardPosition != -1 &&
                        (firstVisible[0] != -1 &&
                                lastVisible[lastVisible.length - 1] != -1) &&
                        (mDetailCardPosition < firstVisible[0] ||
                                mDetailCardPosition > lastVisible[lastVisible.length - 1])) {
                    // If detailed layout is scrolled out of view, then set no recipes to use the
                    // detailed layout
                    mDetailCardPosition = -1;

                    // Utilize listener to trigger CallBack for when the detail layout is no
                    // longer in view
                    if (mVisibilityListener != null) mVisibilityListener.onDetailsHidden();
                }
            }
        });
    }

    @Override
    public void onBindViewHolder(final RecipeViewHolder holder, int position) {
        // Return the data located at the position of the ViewHolder
        Map<String, Object> map = mList.get(position);

        String recipeSourceId = (String) map.get(RecipeEntry.COLUMN_RECIPE_SOURCE_ID);
        String recipeTitle = (String) map.get(RecipeEntry.COLUMN_RECIPE_NAME);
        String recipeAuthor = (String) map.get(RecipeEntry.COLUMN_RECIPE_AUTHOR);
        String recipeAttribution = (String) map.get(RecipeEntry.COLUMN_SOURCE);
        String recipeDescription = (String) map.get(RecipeEntry.COLUMN_SHORT_DESC);
        String recipeImgUrl = (String) map.get(RecipeEntry.COLUMN_IMG_URL);
        int recipeReviews = (int) map.get(RecipeEntry.COLUMN_REVIEWS);
        Double recipeRating = (double) map.get(RecipeEntry.COLUMN_RATING);
        boolean favorite = (boolean) map.get(RecipeEntry.COLUMN_FAVORITE);
        String recipeUrl = (String) map.get(RecipeEntry.COLUMN_RECIPE_URL);

        if (holder.getItemViewType() == RECIPE_VIEW_DETAIL || holder.getItemViewType() == RECIPE_VIEW_DETAIL_LAST) {
            // Instantiate a new RecipeDetailsFragment
            FragmentRecipeDetails fragment = new FragmentRecipeDetails();
            fragment.setCursorLoaderListener(new FragmentRecipeDetails.CursorLoaderListener() {
                @Override
                public void onCursorLoaded(Cursor cursor, int recipeServings) {
                    // When finished loading the fragment, scroll to the recipe
                    holder.itemView.invalidate();
                }
            });

            // Add the recipe's URL as a Bundle to the fragment
            Bundle args = new Bundle();
            args.putParcelable(RECIPE_DETAILS_URL, Uri.parse(recipeUrl));
            args.putString(RECIPE_DETAILS_IMAGE_URL, recipeImgUrl);
            fragment.setArguments(args);

            // Swap the fragment into the container
            mFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();


            // Set the layout to span the entire width
            StaggeredGridLayoutManager.LayoutParams layoutParams =
                    (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(true);
            holder.itemView.setLayoutParams(layoutParams);

        } else {
            // Reset the layout parameters if it has been set differently for the detailed layout
            StaggeredGridLayoutManager.LayoutParams layoutParams =
                    (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(false);
            holder.itemView.setLayoutParams(layoutParams);

            if (recipeImgUrl != null) {
                if (recipeAttribution.equals(mContext.getString(R.string.attribution_custom))) {
                    Glide.with(mContext)
                            .load(recipeImgUrl)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(holder.recipeImage);
                } else {
                    // Use Glide to load image into view
                    Glide.with(mContext)
                            .load(recipeImgUrl)
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .into(holder.recipeImage);
                }
            }

            // Populate the title and description because it is used in all layouts
            holder.recipeTitle.setText(recipeTitle);
            holder.recipeDescription.setText(recipeDescription);

            if (!editMode) {
                if (recipeAttribution.equals(mContext.getString(R.string.attribution_custom)) ||
                        recipeSourceId.contains(mContext.getString(R.string.custom_prefix))) {
                    // Hide non-utilized views if recipe was made or edited by user
                    holder.recipeAttribution.setVisibility(View.INVISIBLE);
                    holder.recipeReviews.setVisibility(View.INVISIBLE);
                    holder.recipeRating.setVisibility(View.INVISIBLE);

                    // If displaying a custom-recipe, then show the overlay to make it stand out
                    holder.overlay.setVisibility(View.VISIBLE);
                } else {
                    // Show the views if recipe was not custom made
                    holder.recipeAttribution.setVisibility(View.VISIBLE);
                    holder.recipeReviews.setVisibility(View.VISIBLE);
                    holder.recipeRating.setVisibility(View.VISIBLE);

                    holder.overlay.setVisibility(View.GONE);
                }

                if (recipeDescription == null || recipeDescription.isEmpty()) {
                    holder.recipeDescription.setVisibility(View.GONE);
                } else {
                    holder.recipeDescription.setVisibility(View.VISIBLE);
                }

                if (recipeReviews == 0) {
                    holder.recipeReviews.setVisibility(View.INVISIBLE);
                    holder.recipeRating.setVisibility(View.INVISIBLE);
                } else {
                    holder.recipeReviews.setVisibility(View.VISIBLE);
                    holder.recipeRating.setVisibility(View.VISIBLE);
                }

                // Populate the rest of the views
                holder.recipeAuthor.setText(Utilities.formatAuthor(mContext, recipeAuthor));
                holder.recipeAttribution.setText(recipeAttribution);
                holder.recipeReviews.setText(Utilities.formatReviews(mContext, recipeReviews));
                holder.recipeRating.setText(Utilities.formatRating(recipeRating));

                // Set the icon of the favorite button depending on its favorite status
                if (favorite) {
                    holder.favoriteButtonOn.setVisibility(View.VISIBLE);
                } else {
                    holder.favoriteButtonOn.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    /**
     * Used to set which ViewHolder should be displaying the DetailedView Fragment as only one can
     * be shown at a time
     * @param position Position of the ViewHolder that should be utilizing DetailedView Fragment
     */
    public void setDetailCardPosition(int position) {
        // Get the position of the ViewHolder currently utilizing the DetailedView Fragment
        int oldPosition = mDetailCardPosition;

        // Set the new position utilizing the DetailedView Fragment
        mDetailCardPosition = position;

        // Notify the Adapter of the change in both the old position and the new position
        notifyItemChanged(oldPosition);
        notifyItemChanged(mDetailCardPosition);
    }

    @Override
    public int getItemCount() {
        if (mList != null) {
            return mList.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (editMode) {
            return RECIPE_VIEW_COMPACT;
        } else if (useDetailView && position == mDetailCardPosition && position == mList.size() - 1) {
            return RECIPE_VIEW_DETAIL_LAST;
        } else if (useDetailView && position == mDetailCardPosition) {
            return RECIPE_VIEW_DETAIL;
        } else if (position == mList.size() - 1) {
             return RECIPE_VIEW_LAST;
        } else {
            return RECIPE_VIEW_NORMAL;
        }
    }

    @Override
    public long getItemId(int position) {
        // ItemId will be the recipeId since it is a UNIQUE primary key
        // Allows for smoother scrolling with StaggeredGridLayout and less shuffling
        return (long) mList.get(position).get(RecipeEntry.COLUMN_RECIPE_ID);
    }

    /**
     * Callback interface for passing information to the UI Activity
     */
    public interface RecipeAdapterOnClickHandler {
        void onClick(String recipeUrl, String imageUrl, RecipeViewHolder viewHolder);
    }

    public interface DetailVisibilityListener {
        void onDetailsHidden();
    }

    public void setVisibilityListener(DetailVisibilityListener mVisibilityListener) {
        this.mVisibilityListener = mVisibilityListener;
    }

    public interface OnStartDragListener {
        void onStartDrag(RecipeViewHolder viewHolder);
    }

    public void setOnStartDragListener(OnStartDragListener listener) {
        mDragListener = listener;
    }

    public class RecipeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @Nullable @BindView(R.id.list_recipe_title) TextView recipeTitle;
        @Nullable @BindView(R.id.list_recipe_author_text) TextView recipeAuthor;
        @Nullable @BindView(R.id.list_recipe_attribution_text) TextView recipeAttribution;
        @Nullable @BindView(R.id.list_recipe_description_text) TextView recipeDescription;
        @Nullable @BindView(R.id.list_recipe_reviews_text) TextView recipeReviews;
        @Nullable @BindView(R.id.list_recipe_rating_text) TextView recipeRating;
        public @Nullable @BindView(R.id.list_recipe_image) ImageView recipeImage;
        @Nullable @BindView(R.id.list_recipe_favorite_button_on) ImageView favoriteButtonOn;
        @Nullable @BindView(R.id.list_recipe_favorite_button_off) ImageView favoriteButtonOff;
        @Nullable @BindView(R.id.fragment_container) FrameLayout container;
        @Nullable @BindView(R.id.list_recipe_image_container) RelativeLayout imageContainer;
//        @Nullable @BindView(R.id.list_recipe_text_container) android.support.v7.widget.GridLayout textContainer;
        @Nullable @BindView(R.id.list_recipe_overlay) FrameLayout overlay;

        @Optional
        @OnTouch(R.id.list_recipe_touchpad)
        boolean onTouchPadTouch(View view, MotionEvent event) {
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                // If the handle is touched, inform the observer to initiate the drag action
                if (mDragListener != null) {
                    mDragListener.onStartDrag(this);
                }
                return true;
            }
            return false;
        }

        @Optional
        @OnClick(R.id.list_recipe_favorite_button_off)
        void favorite(ImageView view) {
            int position = -1;

            // Check to see whether the internal list of recipe information has been initialized
            if (mList.size() > 0) {
                // Retrieve the position of the ViewHolder
                position = getAdapterPosition();
            } else {
                // If not, do nothing
                return;
            }

            // Retrieve the recipe ID to check for favorite status
            long recipeId = (long) mList.get(position).get(RecipeEntry.COLUMN_RECIPE_ID);

            // Set the recipe ID as a tag so it can be retrieved after the animation has finished
            // and the favorite status for the recipe can be toggled
            itemView.setTag(recipeId);

            // Get the favorite status of the
            boolean favorite = (boolean) mList.get(position).get(RecipeEntry.COLUMN_FAVORITE);

            if (favoriteView) {
                mList.remove(position);
                Utilities.setRecipeFavorite(mContext, recipeId);
                notifyItemRemoved(position);
            } else {
                if (favorite) {
                    // If already a favorite, play animation to unfavorite the recipe
                    notifyItemChanged(position, ACTION_UNFAVORITE);
                } else {
                    // If not a favorite, play animation to favorite the recipe
                    notifyItemChanged(position, ACTION_FAVORITE);
                }
            }
        }

        RecipeViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

            // Set the listeners to the ViewHolder
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // Get the recipeId of the RecipeViewHolder just clicked
            int position = getAdapterPosition();
            String recipeUrl = (String) mList.get(position).get(RecipeEntry.COLUMN_RECIPE_URL);
            String imageUrl = (String) mList.get(position).get(RecipeEntry.COLUMN_IMG_URL);

            // Check whether the detail fragment is being utilized
            if (useDetailView) {
                // Reload the view that used to be using the detailed layout

                if (mDetailCardPosition != -1) {
                    // If another recipe is utilizing detail fragment, reset it to use the normal
                    // layout
                    int oldPosition = mDetailCardPosition;
                    notifyItemChanged(oldPosition);

                    if (oldPosition < position) {
                        // If the recipe using the detail fragment is below the previous item utilizing
                        // the detail fragment, scroll to its position so that it will not automatically
                        // be closed by #hideDetails()
                        mRecyclerView.scrollToPosition(oldPosition);
                    }
                }


                // Reload the view
                mDetailCardPosition = position;
                notifyItemChanged(mDetailCardPosition, ACTION_OPEN_DETAILS);
            }

            // Utilize the interface to pass information to the UI thread if detailed view is not
            // being used
            mClickHandler.onClick(recipeUrl, imageUrl, this);
        }
    }
}
