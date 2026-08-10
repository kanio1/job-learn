Playwright

                
# Playwright tips and tricks #1

                    

        
                
                        
                
        

        
                
#### 
                    Adrian Maciuc
                

                
        

        
            
                Dec 8, 2023
                    
                    8 min
            
        

    

                    
        
            Photo by Dan Cristian Pădureț / Unsplash
    
            

            
                
As you work with a framework you start to encounter various situations from which you can learn. Things that you find out are really important and are not that easy to discover just by reading the docs or following tutorials. Things that come with experience. I would like to share some of the things that I have recently learned about Playwright.

I will be referring in the following statements when talking about locator methods in Playwright with `page.locator()` but this does not mean that I suggest to use only `locator()` . Its just a placeholder. On this topic I actually recommend to use the built-in locator methods created by Playwright, mostly `getByTestId()` and if you tried all and it just doesn't work for you, then use `.locator()`

### 1. How to find a child element if only its parents have unique ids

For example if you have `uniqueIDParent &gt; div1 &gt; div2 &gt; span` (where span has the "text you want") , but there are multiple children (spans) with different text, if you give to playwright its parent or grandparent or greatgreat, it will traverse all of its children and extract all text. `expect(uniqueID).toHaveText("text you want")` will work. In the same time `page.getByTestId(uniqueIDParent).filter({ hasText: "text you want" })` will work just fine if you target only the child that has the text  you want. Filtering works miracles.

### 2. Why do I get sometimes in Playwright the error browser has been closed

One reason may be, because you probably forgot an `await` somewhere. Playwright works in asynchronous way, meaning that its all promises, but you need your test steps to run sequentially, top to bottom and to perform the actions on that exact order. This is achieved by using the `await` key that will resolve the promise and this is how Playwright will keep your steps in order, to avoid issues about race conditions. Sometimes VS Code will suggest to you that you don't need some awaits, disregard that, you do need them.

### 3. Auto-waits

Here are some typical examples of assertions from Selenium that you do not need to perform anymore:

-  You don't have to assert element is visible before interacting with it. Playwright will do the following before an interaction: 		- it will check if the element is visible		- if its attached to DOM		- and if its stable, animation is completed
- When you open a page or when you click a link to redirect to a page, you don't have to assert the page is loaded before interacting with elements, playwright will wait for the page to fully load first
- You don't have to write explicit waits for an element when you are waiting for it to appear or waiting for it to disappear. Playwright has built-in timeouts (these explicit waits you know from Selenium) that will wait for an element and try to find it for a set interval of time (eg: by default is 5 seconds for an `expect(locator).toBeVisible()`)
### 4. There are very particular situations sometimes where the timeouts are not working for you

Or you just can't figure it out what to do about it, you still have a last option to simply wait a set number of seconds using the waitForTimeout() . This is discouraged in general, but sometimes you just have no other option.

### 5. How do I assert an array of strings in Playwright ?

`expect(locator).toHaveText(array)` - this can actually take an array and it will, under the hood, iterate over it to assert the items exist

### 6. How to handle multiple elements in Playwright ?

Playwright locators will find both one element or multiple elements with the same method, depends on what you give it. If you want to deal with multiple elements (in Selenium you would do `findElements` and it will return an array of elements) , in Playwright if you do `page.locator(multipleElements)` this will return multiple elements but JUST for playwright under the hood, not for you (at least not as simple array of objects). All you get is one single locator (object). Why this happens? Because you may want to find one single element and try maybe to click it, so when you perform the action, in case the attribute belongs to multiple elements it will not let you click on multiple elements and it will throw an error, suggesting you how to fix your test. But if you really want to deal with all the elements, how do I do that ? You have to add `.all()` at the end. Example: `page.locator(multipleElements).all()`

### 7. How to handle bigger chunks of text ?

Sometimes you can have multiple items that have text, if you give to playwright the parent of all the items, then playwright can extract those texts as an array. You can achieve that with `page.locator(parentOfElementsWithText).allTextContents()` or with `.allInnerTexts()` . The downside of this is that it is not recommended to assert exact match of text, mainly because sometimes it will fetch new lines (/n) or commas or extra spaces, but it can be useful to use this with an `expect(locator).toContain()`

### 8. How do I assert the absence of an element in Playwright ?

You can find this little hack on stackoverflow `expect(locator).toHaveCount(0)`. This is similar to Selenium `findElements`, and if element not found, it just returns an empty array. This is good because it will not fail the test. However I do recommend the Playwright builtin NOT operator , which is what you should actually use. Example `locator(element).not.toBeVisible()`. As a general practice all assert methods can be used with NOT.

### 9. How do I deal with the situation where simple use of just getByTestId() is not enough ? 

There are various scenarios where you have maybe tables, where you need to combine parents and children web elements to reach your desired data. So, before going to `locator()` to build your super complex `css selectors` or `xpath`, think about using the and operator , for example `page.getByText(elem).and(page.getByText(elem)` or you can do `page.getByTestId(elem).getByTestId(elem)` . Another option would be to use filtering locators. Filters also can be used with the mix of NOT operator

### 10. Using one parent element to do multiple actions

You can store the element in a `const element = page.locator()` . Notice that this scenario is one of the few times when you don't need the `await` in front of `page.locator()`. You can look at it as a placeholder. Later in your code you can do `element.click()` or search thru its children `element.getByTestId(child)` . The cool feature about this is that every time you call the element it will re-query the DOM.

### 11. Do not use $(locator) or $$(multiple)

Use of $ or $$ in Playwright is element handle and its highly discouraged by playwright. The main idea about this is that you can encounter situations where using $ will just reference an element from a previous version of the DOM. You may end up seeing the well known Selenium error `StaleElementReferenceException`

### 12. What if you have to wait for the app response and it takes longer than the usual 5 seconds ?

You may encounter this, for example when you perform an action, a click, a submit, and it takes a longer time to get an answer, either you will have the 'spinning loader' or similar. If you know this happens you can increase the timeout to wait for a response on just one particular action, for example `expect(locator).toBeVisible({ timeout: 20000 })` . By passing the timeout inside the method this will override the default playwright configs only for that single line of code (hopefully you will no longer see the  error `error: timed out 5000ms waiting for expect(locator).tobevisible()` and your test will pass)

### 13. Why sometimes expect does not have the usual toHaveText() method ?

But if I try `toBe()` then it works. Because it depends what you are giving to `expect()` as an object. If you give it a standard locator it will have all the web first assertions but if you give it a modified object, something similar to `page.locator(elementWithText).innerText()` , because of the `innerText()` method this is no longer a standard playwright object, and with the rest of the objects it uses the jest expect methods. The playwright expect object will detect what type of object you give it and will work with either the jest expect like `toBe()` or the playwright methods like `toHaveText()`. Just remember that the playwright methods have auto-wait retry and the jest one does not.

### 14. Multiple data test ID attributes on web elements

We can configure in our playwright to use a custom test id attribute. When using the method `getByTestId()` Playwright will default to `data-testid="selector"` , so in case you have a different kind of default id locator set in your web app, you have to set up your testIdAttribute first. This includes if your website has unique ids under the format `id="selector"` . But can you config to use multiple test id attributes? Answer is NO. However, there is a small hack. You can use a different `testIdAttribute` in your general config and have a different one in your projects. Or you can have different projects with different IDs. This is used when you have two teams or group of teams that developed one app using one type of unique ID and another with a different one. It may be a situation of migrating away from a legacy app. Here is below an example

`  projects: [
    {
      name: "new-mega-awesome-app",
      testDir: "./tests",
      use: {
        ...devices["Desktop Chrome"],
        testIdAttribute: "id",
        baseURL: "https://newapp.domain.com",
      },
    },
    {
      name: "legacy-app",
      testDir: "./tests-for-legacy-app",
      use: {
        ...devices["Desktop Chrome"],
        testIdAttribute: "data-testid",
        baseURL: "https://legacyapp.domain.com",
      },
    },
  ],`

Hit the clap button if you found this useful. Or even buy me a coffee if you want to motivate me even more.

Subscribe for more...

Feel free to checkout other nice tips on my blog. I highly recommend this post about publishing your playwright reports directly on github pages, giving you easy access to your run results. Or maybe you would like to try my method in learning new automation frameworks.
Playwright tips and tricks #2I’ve written a post about tips and tricks and it got a lot of love. So, I’ve decided to do another one. 1. How to handle an element that appears after full page load in playwright One of those rare cases where an element will appear in DOM only afterMartioliAdrian MaciucPlaywright tips and tricks #3Get more details about your test during test run. How to use Playwright to test multiple browser windows. Promise.all in PlaywrightMartioliAdrian MaciucPublish your playwright reports to github pagesWhat I am looking to achieve here is a free solution to have my reports published into separate sub-directories, accessible at an unique link that I can easily put in my Jira ticket, github issue or other test management tool, so I can show my client/manager/team what isMartioliAdrian MaciucHow can I learn [more efficient] a new testing framework ?There are so many languages and software testing frameworks out there that sometimes it feels overwhelming. You have Selenium, Cypress, Playwright, TestCafe, Protractor, Robot Framework, TestComplete, Karate, Nightwatch.js, Webdriver.io and many more. You may find yourself in a situation where a new…MartioliAdrian Maciuc
