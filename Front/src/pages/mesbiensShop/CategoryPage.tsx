import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import ShoppingNav from "./ShoppingNav";
import { shop } from "./style";

interface ProductData {
  productNo: number;
  productImg: string;
  productName: string;
  productPrice: number;
  category: string;
}

const CategoryPage: React.FC = () => {
  const { category } = useParams<{ category: string }>();
  const [products, setProducts] = useState<ProductData[]>([]);
  const navigate = useNavigate();

  const handleProductClick = (productNo: number) => {
    navigate(`/shop/product/${productNo}`); // 제품 상세 페이지로 이동
  };

  useEffect(() => {
    const fetchProductData = async () => {
      try {
        const response = await fetch(
          `${process.env.PUBLIC_URL}/dummyDatas/shoppingData.json}`
          // `${process.env.REACT_APP_SERVER_URL}/product/{productNo}`
          // , {
          //   method: "GET",
          //   headers: {
          //     "Content-Type": "application/json",
          //   },
          // }
        );
        const data: ProductData[] = await response.json();
        const filteredProducts = data.filter(
          (item) => category === "All" || item.category === category
        );
        setProducts(filteredProducts);
      } catch (error) {
        console.error("Error fetching product data:", error);
      }
    };

    fetchProductData();
  }, [category]);

  return (
    <shop.MainContainer>
      <ShoppingNav />
      <shop.BodyContainer>
        <shop.ItemContainer>
          {products.map((item) => (
            <shop.Item
              key={item.productNo}
              onClick={() => handleProductClick(item.productNo)}
            >
              <shop.ItemImg src={item.productImg} alt={item.productName} />
              <shop.ItemDescription>
                <li>{item.productName}</li>
                <li>{item.productPrice.toLocaleString()}원</li>
              </shop.ItemDescription>
            </shop.Item>
          ))}
        </shop.ItemContainer>
      </shop.BodyContainer>
    </shop.MainContainer>
  );
};

export default CategoryPage;
