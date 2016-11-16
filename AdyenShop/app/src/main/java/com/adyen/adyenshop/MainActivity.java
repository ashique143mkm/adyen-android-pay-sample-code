package com.adyen.adyenshop;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.adyen.adyenshop.adapter.ProductsAdapter;
import com.adyen.adyenshop.listener.RecyclerItemClickListener;
import com.adyen.adyenshop.model.Product;
import com.adyen.adyenshop.util.PreferencesUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private TextView itemsCount;
    private TextView activeCurrency;
    private FloatingActionButton cartFab;

    private ProductsAdapter productsAdapter;

    private Context context;

    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

    private List<Product> itemsInCartList = new ArrayList<Product>();
    private int itemsInCart;
    private float totalPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(savedInstanceState != null) {
            itemsInCart = savedInstanceState.getInt("itemsInCart");
            itemsInCartList = savedInstanceState.getParcelableArrayList("itemsInCartList");
            totalPrice = savedInstanceState.getFloat("totalPrice");
        } else {
            itemsInCart = 0;
            totalPrice = 0;
        }

        cartFab = (FloatingActionButton)findViewById(R.id.fab);
        mRecyclerView = (RecyclerView) findViewById(R.id.products_recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        productsAdapter = new ProductsAdapter(initializeProductListData(this), this);
        mRecyclerView.setAdapter(productsAdapter);

        initializeView();
        getWindow().setStatusBarColor(getResources().getColor(R.color.black));

        cartFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                proceedToCheckout();
            }
        });
    }

    public void initializeView() {
        context = this;
        PreferencesUtil.getCurrencySharedPreferences(this).edit().clear();

        onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                activeCurrency.setText(PreferencesUtil.getCurrencySharedPreferences(context).getString(getString(R.string.active_currency), "USD"));
                mRecyclerView.invalidate();
                productsAdapter.notifyDataSetChanged();
            }
        };
        PreferencesUtil.registerSharedPreferenceListener(this, onSharedPreferenceChangeListener);

        itemsCount = (TextView) findViewById(R.id.items_count);
        itemsCount.setText(String.valueOf(itemsInCart));

        activeCurrency =(TextView) findViewById(R.id.active_currency);

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(context, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        itemsInCart++;
                        ImageView productIcon = (ImageView) view.findViewById(R.id.product_icon);
                        TextView productName = (TextView) view.findViewById(R.id.product_name);
                        TextView productPrice = (TextView) view.findViewById(R.id.product_price);

                        final Product book = new Product(productName.getText().toString(),
                                Double.valueOf(productPrice.getText().toString().replaceAll("\\$|€|£|R\\$", "")),
                                (Integer) productIcon.getTag());
                        final float bookPriceD = Float.valueOf(productPrice.getText().toString().replaceAll("\\$|€|£|R\\$", ""));
                        totalPrice = totalPrice + bookPriceD;
                        itemsInCartList.add(book);
                        initializeView();
                    }
                })
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("itemsInCart", itemsInCart);
        outState.putFloat("totalPrice", totalPrice);
        outState.putParcelableArrayList("itemsInCartList", new ArrayList<>(itemsInCartList));
    }

    public void proceedToCheckout() {
        Intent intent = new Intent(this, OrderConfirmationActivity.class);
        intent.putExtra("itemsInCart", itemsInCartList.toArray(new Product[itemsInCartList.size()]));
        intent.putExtra("totalPrice", totalPrice);
        startActivity(intent);
    }

    public List<Product> initializeProductListData(Context context) {
        List<Product> products = new ArrayList<>();
        products.add(new Product("Notepad", 0.11, R.mipmap.notepad));
        products.add(new Product("Watch", 0.11, R.mipmap.watch));
        products.add(new Product("Ruler", 0.11, R.mipmap.ruler));
        products.add(new Product("Puzzle", 0.11, R.mipmap.puzzle));
        products.add(new Product("Pencil", 0.11, R.mipmap.pencil));
        products.add(new Product("Pen", 0.11, R.mipmap.pen));
        products.add(new Product("Box cutter", 0.11, R.mipmap.cutter));
        return products;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_currency:
                Dialog currencyDialog = createCurrenciesDialog();
                currencyDialog.show();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onStop() {
        PreferencesUtil.getCurrencySharedPreferences(this).edit().clear();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        PreferencesUtil.getCurrencySharedPreferences(this).edit().clear();
        super.onDestroy();
    }

    private Dialog createCurrenciesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_currency)
                .setItems(R.array.currency_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String[] currencies = getResources().getStringArray(R.array.currency_array);
                        PreferencesUtil.addStringToSharedPreferences(context, getString(R.string.active_currency), currencies[which]);
                    }
                });
        return builder.create();
    }
}
