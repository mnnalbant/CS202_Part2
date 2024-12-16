import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from sqlalchemy import create_engine

# MySQL-specific connection string
DATABASE_URL = "mysql+mysqldb://atatandagidir:password@localhost:3306/hotel_management"  # Modify these credentials

def connect_to_db(url):
    """Create MySQL database connection"""
    try:
        engine = create_engine(url, pool_recycle=3600)
        return engine
    except Exception as e:
        print(f"Error connecting to database: {e}")
        return None

def fetch_and_visualize_data(engine):
    """Fetch data and create visualizations"""
    # Fetching data queries
    room_types_df = pd.read_sql("""
        SELECT type_name, base_capacity, price_per_night 
        FROM RoomType 
        ORDER BY price_per_night DESC;
    """, engine)

    revenue_df = pd.read_sql("""
        SELECT h.hotel_name, r.total_bookings, r.total_revenue 
        FROM RevenueReport r 
        JOIN Hotel h ON r.hotelID = h.hotelID;
    """, engine)

    booking_df = pd.read_sql("""
        SELECT h.hotel_name, r.status, COUNT(*) as room_count
        FROM Room r
        JOIN Hotel h ON r.hotelID = h.hotelID
        GROUP BY h.hotel_name, r.status;
    """, engine)

    # Create visualizations
    plt.style.use('seaborn-v0_8')
    fig = plt.figure(figsize=(15, 10))

    # 1. Room Types and Pricing
    plt.subplot(2, 2, 1)
    ax1 = sns.barplot(data=room_types_df, x='type_name', y='price_per_night')
    plt.xticks(rotation=45, ha='right')
    plt.title('Room Types by Price per Night')
    plt.xlabel('Room Type')
    plt.ylabel('Price ($)')
    for i in ax1.containers:
        ax1.bar_label(i)

    # 2. Hotel Revenue Comparison
    plt.subplot(2, 2, 2)
    ax2 = sns.barplot(data=revenue_df, x='hotel_name', y='total_revenue')
    plt.xticks(rotation=45, ha='right')
    plt.title('Total Revenue by Hotel')
    plt.xlabel('Hotel')
    plt.ylabel('Revenue ($)')
    for i in ax2.containers:
        ax2.bar_label(i)

    # 3. Room Status Distribution
    plt.subplot(2, 2, 3)
    booking_pivot = booking_df.pivot(index='hotel_name', columns='status', values='room_count')
    ax3 = booking_pivot.plot(kind='bar', stacked=True)
    plt.title('Room Status Distribution by Hotel')
    plt.xlabel('Hotel')
    plt.ylabel('Number of Rooms')
    plt.xticks(rotation=45, ha='right')
    plt.legend(title='Status', bbox_to_anchor=(1.05, 1), loc='upper left')

    # 4. Revenue vs Bookings Scatter
    plt.subplot(2, 2, 4)
    sns.scatterplot(data=revenue_df, x='total_bookings', y='total_revenue', s=100)
    plt.title('Revenue vs Number of Bookings')
    plt.xlabel('Total Bookings')
    plt.ylabel('Total Revenue ($)')
    for idx, row in revenue_df.iterrows():
        plt.annotate(row['hotel_name'], 
                     (row['total_bookings'], row['total_revenue']),
                     xytext=(5, 5), textcoords='offset points')

    # Adjust layout and display
    plt.tight_layout()
    plt.show()

    # Print summary statistics
    print("\n=== Summary Statistics ===")
    print("\nRoom Types:")
    print(room_types_df.describe().round(2))
    print("\nRoom Type Details:")
    print(room_types_df.to_string(index=False))
    print("\nRevenue by Hotel:")
    print(revenue_df.to_string(index=False))
    print("\nRoom Status by Hotel:")
    print(booking_df.to_string(index=False))

def main():
    print("Connecting to MySQL database...")
    engine = connect_to_db(DATABASE_URL)
    if engine:
        print("Connection successful! Fetching and visualizing data...")
        fetch_and_visualize_data(engine)
    else:
        print("Failed to connect to database. Please check your credentials and ensure MySQL is running.")

if __name__ == "__main__":
    main()