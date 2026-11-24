Feature: Product search
  As an authenticated organization user
  I want to find products by text, category, status and name order
  So that I can pick the right product from a large catalog

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And the system unit "pc" is selected
    And the following products exist:
      | productName | description       |
      | Whole Milk  | Fresh dairy drink |
      | Butter      | Salted block      |
    And a category named "Household" exists
    And a product group named "Cleaning" exists
    And the following products exist:
      | productName   | description    |
      | Dish Soap     | Lemon scented  |
      | Floor Cleaner | Pine fragrance |

  Scenario: Without any filter the search lists every active product by name
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": {}}
      """
    Then the response status should be 200
    And the response field "contents.productName" should be "[Butter, Dish Soap, Floor Cleaner, Whole Milk]"

  Scenario Outline: Free text matches the name, description and group name however the text is cased
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": {"searchText": "<searchText>"}}
      """
    Then the response status should be 200
    And the response field "contents.productName" should be "<productNames>"

    Examples:
      | searchText | productNames                 |
      | whole milk | [Whole Milk]                 |
      | WHOLE MILK | [Whole Milk]                 |
      | dairy      | [Butter, Whole Milk]         |
      | lemon      | [Dish Soap]                  |
      | cleaning   | [Dish Soap, Floor Cleaner]   |
      | but        | [Butter]                     |
      | buter      | [Butter]                     |
      | zzzzz      | []                           |

  Scenario Outline: Filters narrow the search and combine with the text
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": <parameters>}
      """
    Then the response status should be 200
    And the response field "contents.productName" should be "<productNames>"

    Examples:
      | parameters                                                      | productNames                 |
      | {"categoryIds": ["#category->0"]}                               | [Butter, Whole Milk]         |
      | {"categoryIds": ["#category"]}                                  | [Dish Soap, Floor Cleaner]   |
      | {"categoryIds": ["#category->0", "#category"]}                  | [Butter, Dish Soap, Floor Cleaner, Whole Milk] |
      | {"categoryIds": ["#category"], "searchText": "lemon"}           | [Dish Soap]                  |
      | {"categoryIds": ["#category->0"], "searchText": "lemon"}        | []                           |
      | {"excludeIds": ["#product"]}                                    | [Butter, Dish Soap, Whole Milk] |
      | {"excludeIds": ["#product"], "categoryIds": ["#category"]}      | [Dish Soap]                  |

  Scenario Outline: Only the requested statuses are listed
    When I PUT to /secured/products/#product/deactivate
    Then the response status should be 200
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": <parameters>}
      """
    Then the response status should be 200
    And the response field "contents.productName" should be "<productNames>"

    Examples:
      | parameters                                           | productNames                                   |
      | {}                                                   | [Butter, Dish Soap, Whole Milk]                |
      | {"statusList": ["ACTIVE"]}                           | [Butter, Dish Soap, Whole Milk]                |
      | {"statusList": ["DISCONTINUED"]}                     | [Floor Cleaner]                                |
      | {"statusList": ["ACTIVE", "DISCONTINUED"]}           | [Butter, Dish Soap, Floor Cleaner, Whole Milk] |
      | {"statusList": ["DISCONTINUED"], "searchText": "pine"} | [Floor Cleaner]                              |
      | {"statusList": ["ACTIVE"], "searchText": "pine"}     | []                                             |

  Scenario: Pages follow the name order and the cursor is the last name already seen
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 2, "parameters": {}}
      """
    Then the response status should be 200
    And the response field "contents.productName" should be "[Butter, Dish Soap]"
    And the response field "hasMore" should be "true"
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "Dish Soap", "requestedSize": 2, "parameters": {}}
      """
    Then the response status should be 200
    And the response field "contents.productName" should be "[Floor Cleaner, Whole Milk]"
    And the response field "hasMore" should be "false"

  Scenario: A renamed product is found under its new name only
    When I PUT to /secured/products with payload:
      """json
      {"id": "#product", "productName": "Pine Floor Polish", "description": "Wax finish"}
      """
    Then the response status should be 200
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": {"searchText": "polish"}}
      """
    Then the response field "contents.productName" should be "[Pine Floor Polish]"
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": {"searchText": "fragrance"}}
      """
    Then the response field "contents.productName" should be "[]"

  Scenario: A product moved to another group is found under the new group name only
    Given a product group named "Beverages" exists
    When I PUT to /secured/products with payload:
      """json
      {"id": "#product", "productGroupId": "#productGroup"}
      """
    Then the response status should be 200
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": {"searchText": "beverages"}}
      """
    Then the response field "contents.productName" should be "[Floor Cleaner]"
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": {"searchText": "cleaning"}}
      """
    Then the response field "contents.productName" should be "[Dish Soap]"

  Scenario: Renaming a group makes its products findable under the new group name
    When I PUT to /secured/product-groups with payload:
      """json
      {"id": "#productGroup->0", "groupName": "Chilled Goods"}
      """
    Then the response status should be 200
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": {"searchText": "chilled"}}
      """
    Then the response field "contents.productName" should be "[Butter, Whole Milk]"

  Scenario: Products saved through bulk import are searchable by name, group and category
    When I POST to /secured/products/bulk with payload:
      """json
      {
        "categories": [{"name": "Bakery Goods"}],
        "productGroups": [{"name": "Breads", "categoryName": "Bakery Goods"}],
        "products": [{"name": "Rye Loaf", "description": "Dark sourdough", "groupName": "Breads", "unitCode": "pc"}]
      }
      """
    Then the response status should be 200
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": {"searchText": "breads"}}
      """
    Then the response field "contents.productName" should be "[Rye Loaf]"
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": {"searchText": "sourdough"}}
      """
    Then the response field "contents.productName" should be "[Rye Loaf]"
