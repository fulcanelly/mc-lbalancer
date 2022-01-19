{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE BlockArguments #-}  

module Main where

import Network.Simple.TCP
import Data.Time.Clock.POSIX (getPOSIXTime)
import Control.Applicative (Alternative(many))
import Data.ByteString (ByteString)
import Data.Maybe (fromJust)
import Control.Monad (forever, guard)

import Network.Socket (socketToHandle)

import System.IO

type Sec = Int

data Event
    = Empty
    | Few Int
    deriving Show 

data State 
    = Off 
    | On 
    | Waiting Sec
    deriving Show 

data Context = Ctx {
        maxT :: Int 
        , diff :: Int
    } deriving Show 

stopServer = do
    putStrLn "==> stopping"

startSever = do
    putStrLn "==> starting"

maxWaitSec = 5 

update :: Context -> Event -> State -> IO State 
update Ctx{..} Empty event =
    case event of 
    Off -> pure Off
    On -> pure $ Waiting diff
    Waiting sec -> 
        if sec > maxT then do 
            stopServer 
            pure Off 
        else
            pure $ Waiting (sec + diff) 
            
update _ (Few _) event =
    case event of 
    Off -> do
        startSever
        pure On
    _ -> pure On 


toEvent n = if n == 0 then Empty else Few n

fetchEvent = connect "localhost" "1344" onConn where
    runConn :: Handle -> State -> Int -> IO ()
    runConn h state time = do 
        h `hPutStrLn` "get_online"
        event <- toEvent . read <$> hGetLine h
       
      --  state <- update Ctx{10, diff'} event state 

        time' <- getTime
        let diff' = time' - time
        
        print state 
        putStrLn $ "got: " <> show event
        putStrLn $ "time took: " <> show diff'
       
        state <- update (Ctx maxWaitSec diff') event state  

        runConn h state time' 

    onConn (socket, addr) = do
        h <- socketToHandle socket ReadWriteMode         
        runConn h Off =<< getTime

getTime = round <$> getPOSIXTime

main :: IO ()
main = do
    let time = round <$> getPOSIXTime
    fetchEvent
