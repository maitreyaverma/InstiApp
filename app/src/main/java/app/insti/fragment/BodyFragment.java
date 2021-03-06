package app.insti.fragment;


import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import app.insti.Constants;
import app.insti.ItemClickListener;
import app.insti.MainActivity;
import app.insti.R;
import app.insti.ShareURLMaker;
import app.insti.adapter.BodyAdapter;
import app.insti.adapter.FeedAdapter;
import app.insti.adapter.UserAdapter;
import app.insti.api.RetrofitInterface;
import app.insti.api.ServiceGenerator;
import app.insti.data.AppDatabase;
import app.insti.data.Body;
import app.insti.data.Event;
import app.insti.data.Role;
import app.insti.data.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.noties.markwon.Markwon;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BodyFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BodyFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


    String TAG = "BodyFragment";
    private AppDatabase appDatabase;
    // TODO: Rename and change types of parameters
    private Body min_body;
    private SwipeRefreshLayout bodySwipeRefreshLayout;


    public BodyFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param arg_body Body for details
     * @return A new instance of fragment BodyFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BodyFragment newInstance(Body arg_body) {
        BodyFragment fragment = new BodyFragment();
        Bundle args = new Bundle();
        args.putString(Constants.BODY_JSON, new Gson().toJson(arg_body));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            min_body = new Gson().fromJson(getArguments().getString(Constants.BODY_JSON), Body.class);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        /* Initialize */
        appDatabase = AppDatabase.getAppDatabase(getContext());
        displayBody(min_body);
        new getDbBody().execute(min_body.getBodyID());
        bodySwipeRefreshLayout = getActivity().findViewById(R.id.body_swipe_refresh_layout);
        bodySwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateBody();
            }
        });

        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(min_body.getBodyName());
    }

    private void updateBody() {
        RetrofitInterface retrofitInterface = ServiceGenerator.createService(RetrofitInterface.class);
        retrofitInterface.getBody(((MainActivity) getActivity()).getSessionIDHeader(), min_body.getBodyID()).enqueue(new Callback<Body>() {
            @Override
            public void onResponse(Call<Body> call, Response<Body> response) {
                if (response.isSuccessful()) {
                    Body body = response.body();

                    new updateDbBody().execute(body);

                    displayBody(body);
                    bodySwipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(Call<Body> call, Throwable t) {
                bodySwipeRefreshLayout.setRefreshing(false);
                // Network Error
            }
        });
    }

    private void setVisibleIfHasElements(int[] viewIds, List list) {
        if (list != null && list.size() > 0) {
            for (int viewId: viewIds){
                getActivity().findViewById(viewId).setVisibility(View.VISIBLE);
            }
        }
    }

    private void displayBody(final Body body) {
        /* Skip if we're already destroyed */
        if (getView() == null) return;

        TextView bodyName = (TextView) getView().findViewById(R.id.body_name);
        TextView bodyDescription = (TextView) getView().findViewById(R.id.body_description);
        ImageView eventPicture = (ImageView) getActivity().findViewById(R.id.body_picture);
        ImageButton webBodyButton = getActivity().findViewById(R.id.web_body_button);
        ImageButton shareBodyButton = getActivity().findViewById(R.id.share_body_button);
        final Button followButton = getActivity().findViewById(R.id.follow_button);

        /* Show relevant card titles */
        setVisibleIfHasElements(new int[]{R.id.body_events_title, R.id.event_card_recycler_view}, body.getBodyEvents());
        setVisibleIfHasElements(new int[]{R.id.body_orgs_title, R.id.org_card_recycler_view}, body.getBodyChildren());
        setVisibleIfHasElements(new int[]{R.id.body_parents_title, R.id.parentorg_card_recycler_view}, body.getBodyParents());
        setVisibleIfHasElements(new int[]{R.id.body_people_title, R.id.people_card_recycler_view}, body.getBodyRoles());

        /* Set body information */
        bodyName.setText(body.getBodyName());
        Picasso.get().load(body.getBodyImageURL()).into(eventPicture);

        /* Return if it's a min body */
        if (body.getBodyDescription() == null) {
            return;
        }

        Markwon.setMarkdown(bodyDescription, body.getBodyDescription());

        /* Check if user is already following
         * Initialize follow button */
        followButton.setBackgroundColor(getResources().getColor(body.getBodyUserFollows() ? R.color.colorAccent : R.color.colorWhite));

        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RetrofitInterface retrofitInterface = ServiceGenerator.createService(RetrofitInterface.class);
                retrofitInterface.updateBodyFollowing(((MainActivity) getActivity()).getSessionIDHeader(), body.getBodyID(), body.getBodyUserFollows() ? 0 : 1).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            body.setBodyUserFollows(!body.getBodyUserFollows());
                            new updateDbBody().execute(body);
                            followButton.setBackgroundColor(getResources().getColor(body.getBodyUserFollows() ? R.color.colorAccent : R.color.colorWhite));
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(getContext(), "Network Error", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        /* Initialize web button */
        if (body.getBodyWebsiteURL() != null && !body.getBodyWebsiteURL().isEmpty()) {
            webBodyButton.setVisibility(View.VISIBLE);
            webBodyButton.setOnClickListener(new View.OnClickListener() {
                String bodywebURL = body.getBodyWebsiteURL();

                @Override
                public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(bodywebURL));
                    startActivity(browserIntent);
                }
            });
        }

        /* Initialize share button */
        shareBodyButton.setOnClickListener(new View.OnClickListener() {
            String shareUrl = ShareURLMaker.getBodyURL(body);

            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL");
                i.putExtra(Intent.EXTRA_TEXT, shareUrl);
                startActivity(Intent.createChooser(i, "Share URL"));
            }
        });

        /* Initialize events */
        final List<Event> eventList = body.getBodyEvents();
        RecyclerView eventRecyclerView = (RecyclerView) getActivity().findViewById(R.id.event_card_recycler_view);
        FeedAdapter eventAdapter = new FeedAdapter(eventList, new ItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Event event = eventList.get(position);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.EVENT_JSON, new Gson().toJson(event));
                EventFragment eventFragment = new EventFragment();
                eventFragment.setArguments(bundle);
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_right);
                ft.replace(R.id.framelayout_for_fragment, eventFragment, eventFragment.getTag());
                ft.addToBackStack(eventFragment.getTag());
                ft.commit();
            }
        });
        eventRecyclerView.setAdapter(eventAdapter);
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        /* Get users from roles */
        final List<Role> roles = body.getBodyRoles();
        final List<User> users = new ArrayList();
        for (Role role : roles) {
            if (role.getRoleUsersDetail() != null) {
                for (User user : role.getRoleUsersDetail()) {
                    user.setCurrentRole(role.getRoleName());
                    users.add(user);
                }
            }
        }

        /* Initialize People */
        RecyclerView userRecyclerView = (RecyclerView) getActivity().findViewById(R.id.people_card_recycler_view);
        UserAdapter userAdapter = new UserAdapter(users, new ItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                User user = users.get(position);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.USER_ID, user.getUserID());
                ProfileFragment profileFragment = new ProfileFragment();
                profileFragment.setArguments(bundle);
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_right);
                ft.replace(R.id.framelayout_for_fragment, profileFragment, profileFragment.getTag());
                ft.addToBackStack(profileFragment.getTag());
                ft.commit();
            }
        });
        userRecyclerView.setAdapter(userAdapter);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        /* Initialize Parent bodies */
        RecyclerView parentsRecyclerView = (RecyclerView) getActivity().findViewById(R.id.parentorg_card_recycler_view);
        BodyAdapter parentAdapter = new BodyAdapter(body.getBodyParents(), new ItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                openBody(body.getBodyParents().get(position));
            }
        });
        parentsRecyclerView.setAdapter(parentAdapter);
        parentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        /* Initialize child bodies */
        RecyclerView childrenRecyclerView = (RecyclerView) getActivity().findViewById(R.id.org_card_recycler_view);
        BodyAdapter childrenAdapter = new BodyAdapter(body.getBodyChildren(), new ItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                openBody(body.getBodyChildren().get(position));
            }
        });
        childrenRecyclerView.setAdapter(childrenAdapter);
        childrenRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        getActivity().findViewById(R.id.loadingPanel).setVisibility(View.GONE);
    }

    /**
     * Open body fragment for a body
     */
    private void openBody(Body body) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.BODY_JSON, new Gson().toJson(body));
        BodyFragment bodyFragment = new BodyFragment();
        bodyFragment.setArguments(bundle);
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_right);
        ft.replace(R.id.framelayout_for_fragment, bodyFragment, bodyFragment.getTag());
        ft.addToBackStack(bodyFragment.getTag());
        ft.commit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_body, container, false);
    }

    private class updateDbBody extends AsyncTask<Body, Void, Integer> {
        @Override
        protected Integer doInBackground(Body... body) {
            if (appDatabase.dbDao().getBody(body[0].getBodyID()).length > 0) {
                appDatabase.dbDao().updateBody(body[0]);
            } else {
                appDatabase.dbDao().insertBody(body[0]);
            }
            return 1;
        }
    }

    private class getDbBody extends AsyncTask<String, Void, Body[]> {
        @Override
        protected Body[] doInBackground(String... id) {
            return appDatabase.dbDao().getBody(min_body.getBodyID());
        }

        @Override
        protected void onPostExecute(Body[] result) {
            if (result.length > 0) {
                displayBody(result[0]);
            } else {
                updateBody();
            }
        }
    }

}
